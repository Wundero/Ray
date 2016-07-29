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
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.achievement.GrantAchievementEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.KickPlayerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.message.MessageEvent;
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
import org.spongepowered.api.text.format.TextColors;

import me.Wundero.ProjectRay.Ray;
import me.Wundero.ProjectRay.framework.Format;
import me.Wundero.ProjectRay.framework.FormatType;
import me.Wundero.ProjectRay.framework.Group;
import me.Wundero.ProjectRay.framework.RayPlayer;
import me.Wundero.ProjectRay.framework.channel.ChatChannel;
import me.Wundero.ProjectRay.utils.Utils;

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
		if (f.getTemplate() == null) {
			// possibly not loaded, but i don't care to figure out why formats
			// don't always properly load templates
			return true;
		}
		if (channel instanceof ChatChannel) {
			ChatChannel c = (ChatChannel) channel;
			v.put("channel", c.getTag());
			v.put("channelname", c.getName());
		}
		v = Ray.get().setVars(v, f.getTemplate(), p, false, Optional.of(f), true);
		final TextTemplate template = f.getTemplate();
		final Map<String, Object> args = Utils.sm(v);
		MessageChannel newchan = MessageChannel.combined(channel, new MessageChannel() {

			@Override
			public Optional<Text> transformMessage(Object sender, MessageReceiver recipient, Text original,
					ChatType type) {

				if (recipient instanceof Player && sender instanceof Player) {
					Player s = (Player) sender;
					Player r = (Player) recipient;
					if (RayPlayer.getRay(r).isIgnoring(RayPlayer.getRay(s))) {
						return Optional.empty();
					}
				}

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
		Map<String, Object> vars = Utils.sm();
		Player p = null;
		if (event.getCause().containsType(Player.class)) {
			p = (Player) event.getCause().first(Player.class).get();
		}
		vars.put("message", Utils.transIf(event.getRawMessage().toPlain(), p));
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
				}
			}).submit(Ray.get().getPlugin());
			Task.builder().delayTicks(20).execute(() -> {
				MessageChannelEvent.Chat ev2 = SpongeEventFactory.createMessageChannelEventChat(
						Cause.builder().from(event.getCause()).named("formattype", FormatType.JOIN).build(),
						event.getChannel().get(), event.getChannel(), event.getFormatter(), event.getMessage(), false);
				Sponge.getEventManager().post(ev2);
				if (!ev2.isCancelled()) {
					ev2.getChannel().get().send(event.getTargetEntity(), ev2.getMessage(), ChatTypes.CHAT);
				}
			}).submit(Ray.get().getPlugin());
		}
		Task.builder().delayTicks(15).execute(() -> {
			MessageChannelEvent.Chat ev2 = SpongeEventFactory.createMessageChannelEventChat(
					Cause.builder().from(event.getCause()).named("formattype", FormatType.MOTD).build(),
					MessageChannel.fixed(event.getTargetEntity()),
					Optional.of(MessageChannel.fixed(event.getTargetEntity())),
					new MessageEvent.MessageFormatter(Text.of(TextColors.LIGHT_PURPLE, "Welcome to the server!")),
					event.getMessage(), false);
			Sponge.getEventManager().post(ev2);
			if (!ev2.isCancelled()) {
				ev2.getChannel().get().send(event.getTargetEntity(), ev2.getMessage(), ChatTypes.CHAT);
			}
		}).submit(Ray.get().getPlugin());
	}

	// Logs ALL commands that are handled by the server
	@Listener
	public void onCommand(SendCommandEvent event) {
		String player = "";
		if (event.getCause().containsType(CommandSource.class)) {
			player = event.getCause().first(CommandSource.class).get().getName() + ": ";
		}
		Ray.get().getLogger().info(player + "/" + event.getCommand() + " " + event.getArguments());
	}

	@Listener
	public void onQuit(ClientConnectionEvent.Disconnect event) {
		Map<String, Object> vars = Utils.sm();
		event.setMessageCancelled(
				handle(FormatType.LEAVE, event, vars, event.getTargetEntity(), event.getChannel().get()));
	}

	// TODO figure out way to customize messages - locale is retrivable so use
	// translations?
	@Listener
	public void onDeath(DestructEntityEvent.Death event) {
		if (!(event.getTargetEntity() instanceof Player)) {
			return;
		}
		Map<String, Object> vars = Utils.sm();
		event.setMessageCancelled(
				handle(FormatType.DEATH, event, vars, (Player) event.getTargetEntity(), event.getChannel().get()));
	}

	@Listener
	public void onKick(KickPlayerEvent event) {
		Map<String, Object> vars = Utils.sm();
		event.setMessageCancelled(
				handle(FormatType.KICK, event, vars, event.getTargetEntity(), event.getChannel().get()));
	}

	@Listener
	public void onAch(GrantAchievementEvent.TargetPlayer event) {
		Map<String, Object> vars = Utils.sm();
		Achievement ach = event.getAchievement();
		vars.put("achievement",
				Text.builder().append(Text.of(ach.getName())).onHover(TextActions.showAchievement(ach)).build());
		event.setMessageCancelled(
				handle(FormatType.ACHIEVEMENT, event, vars, event.getTargetEntity(), event.getChannel().get()));

	}

}
