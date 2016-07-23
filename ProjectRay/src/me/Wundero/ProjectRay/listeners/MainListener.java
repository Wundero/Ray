package me.Wundero.ProjectRay.listeners;
/*
 The MIT License (MIT)

 Copyright (c) 2016 Wundero

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.achievement.GrantAchievementEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.KickPlayerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.statistic.achievement.Achievement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.chat.ChatTypes;

import com.google.common.collect.Maps;

import me.Wundero.ProjectRay.Ray;
import me.Wundero.ProjectRay.framework.Format;
import me.Wundero.ProjectRay.framework.FormatType;
import me.Wundero.ProjectRay.framework.Group;
import me.Wundero.ProjectRay.framework.RayPlayer;

public class MainListener {

	private boolean handle(FormatType t, MessageChannelEvent e, Map<String, Object> v, final Player p,
			MessageChannel channel) {
		if (p == null) {
			return false;
		}
		RayPlayer r = RayPlayer.getRay(p);
		Group g = r.getActiveGroup();
		if (g == null) {
			return false;
		}
		final Format f = g.getFormat(t);
		if (f == null) {
			return false;
		}
		v = Ray.get().setVars(v, f.getTemplate(), p, false, Optional.of(f), true);
		final TextTemplate template = f.getTemplate();
		final Map<String, Object> args = Maps.newHashMap(v);
		MessageChannel newchan = MessageChannel.combined(channel, new MessageChannel() {

			@Override
			public Optional<Text> transformMessage(Object sender, MessageReceiver recipient, Text original,
					ChatType type) {
				Ray.get().getLogger().info("Original message: " + original.toPlain());
				if (recipient instanceof Player) {
					args.putAll(Ray.get().setVars(args, template, (Player) recipient, true, Optional.of(f), true));
				} else {
					args.putAll(Ray.get().setVars(args, template, null, true, Optional.of(f), true));
				}
				if (template == null) {
					return Optional.of(original);
				}
				Text t = template.apply(args).build();
				return Optional.of(t);
			}

			@Override
			public Collection<MessageReceiver> getMembers() {
				return Collections.emptyList();
			}

		});
		e.setChannel(newchan);
		return false;
	}

	@Listener
	public void onChat(MessageChannelEvent.Chat event) {
		Map<String, Object> vars = Maps.newHashMap();
		vars.put("message", event.getRawMessage());
		Player p = null;
		if (event.getCause().containsType(Player.class)) {
			p = (Player) event.getCause().first(Player.class).get();
		}
		if (event.getCause().containsNamed("formattype")) {
			event.setCancelled(handle(event.getCause().get("formattype", FormatType.class).get(), event, vars, p,
					event.getChannel().get()));
		} else {
			event.setCancelled(handle(FormatType.CHAT, event, vars, p, event.getChannel().get()));
		}
	}

	@Listener
	public void onJoin(ClientConnectionEvent.Join event) {
		Ray.get().setLoadable(event.getTargetEntity());
		if (event.getChannel().isPresent()) {
			event.setMessageCancelled(true);
			Task.builder().delayTicks(10).execute(() -> {
				MessageChannelEvent.Chat ev2 = SpongeEventFactory.createMessageChannelEventChat(
						Cause.builder().from(event.getCause()).named("formattype", FormatType.JOIN).build(),
						event.getChannel().get(), event.getChannel(), event.getFormatter(), event.getMessage(), false);
				Sponge.getEventManager().post(ev2);
				if (!ev2.isCancelled()) {
					ev2.getChannel().get().send(event.getTargetEntity(), ev2.getMessage(), ChatTypes.CHAT);
				}
			}).submit(Ray.get().getPlugin());
		}
	}

	@Listener
	public void onQuit(ClientConnectionEvent.Disconnect event) {
		Map<String, Object> vars = Maps.newHashMap();
		event.setMessageCancelled(
				handle(FormatType.LEAVE, event, vars, event.getTargetEntity(), event.getChannel().get()));
	}

	@Listener
	public void onDeath(DestructEntityEvent.Death event) {
		if (!(event.getTargetEntity() instanceof Player)) {
			return;
		}
		Map<String, Object> vars = Maps.newHashMap();
		Ray.get().getPlugin().getLogger().info("Cause: " + event.getCause().all());
		event.setMessageCancelled(
				handle(FormatType.DEATH, event, vars, (Player) event.getTargetEntity(), event.getChannel().get()));
	}

	@Listener
	public void onKick(KickPlayerEvent event) {
		Map<String, Object> vars = Maps.newHashMap();
		event.setMessageCancelled(
				handle(FormatType.LEAVE, event, vars, event.getTargetEntity(), event.getChannel().get()));
	}

	@Listener
	public void onAch(GrantAchievementEvent.TargetPlayer event) {
		Map<String, Object> vars = Maps.newHashMap();
		Achievement ach = event.getAchievement();
		vars.put("achievement",
				Text.builder().append(Text.of(ach.getName())).onHover(TextActions.showAchievement(ach)).build());
		event.setMessageCancelled(
				handle(FormatType.ACHIEVEMENT, event, vars, event.getTargetEntity(), event.getChannel().get()));

	}

}
