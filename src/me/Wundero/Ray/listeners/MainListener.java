package me.Wundero.Ray.listeners;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabList;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.achievement.GrantAchievementEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.KickPlayerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.statistic.achievement.Achievement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;

import me.Wundero.Ray.Ray;
import me.Wundero.Ray.framework.Group;
import me.Wundero.Ray.framework.RayCombinedMessageChannel;
import me.Wundero.Ray.framework.channel.ChatChannel;
import me.Wundero.Ray.framework.format.ExecutingFormat;
import me.Wundero.Ray.framework.format.Format;
import me.Wundero.Ray.framework.format.type.FormatType;
import me.Wundero.Ray.framework.format.type.FormatTypes;
import me.Wundero.Ray.framework.player.RayPlayer;
import me.Wundero.Ray.utils.TextUtils;
import me.Wundero.Ray.utils.Utils;
import me.Wundero.Ray.utils.ValueHolder;
import me.Wundero.Ray.variables.ParsableData;

public class MainListener {

	private boolean handle(FormatType t, MessageChannelEvent e, Map<String, Object> v, final Player p,
			MessageChannel channel) {
		return handle(t, e, v, p, channel, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
	}

	private boolean handle(FormatType t, MessageChannelEvent e, Map<String, Object> v, final Player p,
			MessageChannel channel, Optional<Player> msgsender, Optional<Player> msgrecip, Optional<String> formatName,
			Optional<Player> observer) {
		if (p == null) {
			return false;
		}
		RayPlayer r = RayPlayer.getRay(p);
		Group g = r.getActiveGroup();
		if (g == null) {
			return false;
		}
		Format fx;
		if (formatName.isPresent()) {
			fx = g.getFormat(t, formatName.get());
		} else {
			fx = g.getFormat(t);
		}
		if (fx == null) {
			return false;
		}
		final Format f = fx;
		final UUID exf = UUID.randomUUID();
		final Optional<ExecutingFormat> ef = f instanceof ExecutingFormat ? Optional.of((ExecutingFormat) f)
				: f.getInternal(ExecutingFormat.class);
		final Map<String, Object> args = Utils.sm(v);
		ChatChannel pc = r.getActiveChannel();
		boolean obfuscate = pc != null && pc.isObfuscateRanged();
		double range = pc == null ? -1 : pc.range();
		// note that MessageChannel channel is inconsistent and can be many
		// things. Do not infer types with it.

		// RayCombinedMessageChannel allows an individual channel to delete the
		// message.
		MessageChannel newchan = new RayCombinedMessageChannel(channel, new MessageChannel() {
			@Override
			public Optional<Text> transformMessage(Object sender, MessageReceiver recipient, Text original,
					ChatType type) {
				Map<String, Object> mc = Utils.sm(args);
				if (recipient instanceof Player && sender instanceof Player) {
					Player s = (Player) sender;
					Player r = (Player) recipient;
					if (RayPlayer.getRay(r).isIgnoring(RayPlayer.getRay(s))) {
						return Optional.empty();
					}
					if (obfuscate && !Utils.inRange(r.getLocation(), s.getLocation(), range)) {
						double delta = Utils.difference(s.getLocation(), r.getLocation());
						// percent difference calc adjusted to make the percent
						// larger but never above 100 (range is 10% smaller than
						// normal)
						// lim(delta->infinity) (func %dc) = 100
						double percentDiscoloration = ((delta - (range / 1.1)) / delta) * 100;
						double percentObfuscation = Math.sqrt(percentDiscoloration);
						Text m = TextUtils.obfuscate((Text) mc.get("message"), percentObfuscation,
								percentDiscoloration);
						mc.put("message", m);
					} else if (!Utils.inRange(s.getLocation(), r.getLocation(), range)) {
						return Optional.empty();
					}
				}
				ef.ifPresent((format) -> {
					format.execConsoles(exf, 1000);
				});
				ValueHolder<Text> vv = new ValueHolder<Text>();
				if (!f.send((text) -> {
					if (vv.getValue() != null) {
						return false;
					}
					vv.setValue(text);
					return true;
				}, new ParsableData().setKnown(mc).setSender(msgsender.orElse(p))
						.setRecipient(msgrecip.orElse(recipient instanceof Player ? (Player) recipient : null))
						.setClickHover(true).setObserver(observer.isPresent() ? observer
								: recipient instanceof Player ? Optional.of((Player) recipient) : Optional.empty()))) {
					return Optional.empty();
				}
				return Optional.of(vv.getValue());
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
		vars.put("message", TextUtils.transIf(event.getRawMessage().toPlain(), p));
		if (event.getCause().containsNamed("formattype")) {
			Optional<Player> sf = Optional.empty();
			Optional<Player> st = Optional.empty();
			Optional<String> fn = Optional.empty();
			Optional<Player> o = Optional.empty();
			if (event.getCause().containsNamed("sendfrom")) {
				sf = event.getCause().get("sendfrom", Player.class);
			}
			if (event.getCause().containsNamed("sendto")) {

				st = event.getCause().get("sendto", Player.class);
			}
			if (event.getCause().containsNamed("formatname")) {
				fn = event.getCause().get("formatname", String.class);
			}
			if (event.getCause().contains("observer")) {
				o = event.getCause().get("observer", Player.class);
			}
			event.setCancelled(handle(event.getCause().get("formattype", FormatType.class).get(), event, vars, p,
					event.getChannel().get(), sf, st, fn, o));
		} else {
			event.setCancelled(handle(FormatTypes.CHAT, event, vars, p, event.getChannel().get()));
		}
	}

	@Listener(order = Order.LATE)
	public void onJoin(ClientConnectionEvent.Join event) {
		boolean welcome = !event.getTargetEntity().hasPlayedBefore();
		final RayPlayer p = RayPlayer.get(event.getTargetEntity());
		p.setTabTask(() -> {
			Player player = event.getTargetEntity();
			final TabList list = player.getTabList();
			List<TabListEntry> lx = Utils.sl(list.getEntries());
			for (TabListEntry e : lx) {
				Optional<Player> h = Sponge.getServer().getPlayer(e.getProfile().getUniqueId());
				if (!h.isPresent()) {
					continue;
				}
				Player pla = h.get();
				RayPlayer plx = RayPlayer.get(pla);
				Group g = plx.getActiveGroup();
				if (g == null) {
					continue;
				}
				Format f = g.getFormat(FormatTypes.TABLIST_ENTRY);
				if (f == null) {
					continue;
				}
				f.send((text) -> {
					TabListEntry e2 = e;
					if (!list.getEntry(e.getProfile().getUniqueId()).isPresent()) {
						return false;
					}
					if (!(list.getEntry(e.getProfile().getUniqueId()).get().equals(e))) {
						e2 = list.getEntry(e.getProfile().getUniqueId()).get();
					}
					e2.setDisplayName(text);
					return true;
				}, new ParsableData().setClickHover(false).setSender(pla).setRecipient(player));
			}
		});
		p.startTabHFTask();
		final Group g = p.getActiveGroup();
		if (g != null) {
			Task.builder().delayTicks(20).execute(() -> {
				Format h = g.getFormat(FormatTypes.TABLIST_HEADER);
				Format f = g.getFormat(FormatTypes.TABLIST_FOOTER);
				if (h != null) {
					h.send(text -> {
						p.queueHeader(text);
						return true;
					}, new ParsableData().setClickHover(false).setSender(event.getTargetEntity())
							.setRecipient(event.getTargetEntity()));
				}
				if (f != null) {
					f.send(text -> {
						p.queueHeader(text);
						return true;
					}, new ParsableData().setClickHover(false).setSender(event.getTargetEntity())
							.setRecipient(event.getTargetEntity()));
				}
			}).submit(Ray.get().getPlugin());
		}

		if (event.getChannel().isPresent()) {
			event.setMessageCancelled(true);
			Task.builder().delayTicks(10).execute(() -> {
				MessageChannelEvent.Chat ev2 = SpongeEventFactory.createMessageChannelEventChat(
						Cause.builder().from(event.getCause()).named("formattype", FormatTypes.JOIN).build(),
						event.getChannel().get(), event.getChannel(), event.getFormatter(), event.getMessage(), false);
				Sponge.getEventManager().post(ev2);
				if (!ev2.isCancelled()) {
				}
			}).submit(Ray.get().getPlugin());
			Task.builder().delayTicks(20).execute(() -> {
				MessageChannelEvent.Chat ev2 = SpongeEventFactory.createMessageChannelEventChat(
						Cause.builder().from(event.getCause()).named("formattype", FormatTypes.JOIN).build(),
						event.getChannel().get(), event.getChannel(), event.getFormatter(), event.getMessage(), false);
				Sponge.getEventManager().post(ev2);
				if (!ev2.isCancelled()) {
					ev2.getChannel().get().send(event.getTargetEntity(), ev2.getMessage(), ChatTypes.CHAT);
				}
			}).submit(Ray.get().getPlugin());
		}
		if (welcome) {
			Task.builder().delayTicks(15).execute(() -> {
				MessageChannelEvent.Chat ev2 = SpongeEventFactory.createMessageChannelEventChat(
						Cause.builder().from(event.getCause()).named("formattype", FormatTypes.WELCOME).build(),
						MessageChannel.TO_ALL, Optional.of(MessageChannel.TO_ALL),
						new MessageEvent.MessageFormatter(Text.of(TextColors.LIGHT_PURPLE,
								"Welcome " + event.getTargetEntity().getName() + " to the server!")),
						event.getMessage(), false);
				Sponge.getEventManager().post(ev2);
				if (!ev2.isCancelled()) {
					ev2.getChannel().get().send(event.getTargetEntity(), ev2.getMessage(), ChatTypes.CHAT);
				}
			}).submit(Ray.get().getPlugin());
		}
		Task.builder().delayTicks(15).execute(() -> {
			MessageChannelEvent.Chat ev2 = SpongeEventFactory.createMessageChannelEventChat(
					Cause.builder().from(event.getCause()).named("formattype", FormatTypes.MOTD).build(),
					MessageChannel.fixed(event.getTargetEntity()),
					Optional.of(MessageChannel.fixed(event.getTargetEntity())),
					new MessageEvent.MessageFormatter(Text.of(TextColors.LIGHT_PURPLE, "Welcome to the server!")),
					event.getMessage(), false);
			Sponge.getEventManager().post(ev2);
			if (!ev2.isCancelled()) {
				ev2.getChannel().get().send(event.getTargetEntity(), ev2.getMessage(), ChatTypes.CHAT);
			}
		}).submit(Ray.get().getPlugin());
		Task.builder().delayTicks(30).execute(() -> {
			RayPlayer.updateTabs();
			p.updateTab();
		}).submit(Ray.get().getPlugin());
	}

	// Logs ALL commands that are handled by the server
	@Listener(order = Order.POST)
	public void onCommand(SendCommandEvent event) {
		String player = "";
		CommandSource trs = null;
		if (event.getCause().containsType(CommandSource.class)) {
			trs = Utils.getTrueSource(event.getCause().first(CommandSource.class).get());
			player = trs.getName() + ": ";
		} else {
			Ray.get().getLogger().info(player + "/" + event.getCommand() + " " + event.getArguments());
			return;
		}
		Ray.get().getLogger().info(player + "/" + event.getCommand() + " " + event.getArguments());
		if (!(trs instanceof Player)) {
			return;
		}
		if (!Ray.get().getConfig().getNode("spy", event.getCommand()).getBoolean(false)) {
			return;
		}
		Player sendto = (Player) trs;
		Text msg = Text.of(player + "/" + event.getCommand() + " " + event.getArguments());

		List<MessageReceiver> spies = Utils.sl();
		for (Player p : Sponge.getServer().getOnlinePlayers()) {
			RayPlayer r = RayPlayer.get(p);
			if (r.spy() && !p.getName().equals(player)) {
				spies.add(p);
			}
		}
		MessageChannelEvent.Chat spyevent = SpongeEventFactory.createMessageChannelEventChat(
				Cause.source(Ray.get()).named("formattype", FormatTypes.MESSAGE_SPY).named("sendfrom", sendto).build(),
				sendto.getMessageChannel(),
				Optional.of(MessageChannel.combined(MessageChannel.fixed(spies), MessageChannel.TO_CONSOLE)),
				new MessageEvent.MessageFormatter(Text.of(msg)), Text.of(msg), false);
		if (Utils.call(spyevent)) {
			spyevent.getChannel().get().send(sendto, spyevent.getMessage());
		}
	}

	@Listener
	public void onQuit(ClientConnectionEvent.Disconnect event) {
		Map<String, Object> vars = Utils.sm();
		event.setMessageCancelled(
				handle(FormatTypes.LEAVE, event, vars, event.getTargetEntity(), event.getChannel().get()));
		RayPlayer.updateTabs();
	}

	@Listener
	public void onKick(KickPlayerEvent event) {
		Map<String, Object> vars = Utils.sm();
		event.setMessageCancelled(
				handle(FormatTypes.KICK, event, vars, event.getTargetEntity(), event.getChannel().get()));
		RayPlayer.updateTabs();
	}

	@Listener
	public void onAch(GrantAchievementEvent.TargetPlayer event) {
		Map<String, Object> vars = Utils.sm();
		Achievement ach = event.getAchievement();
		vars.put("achievement",
				Text.builder().append(Text.of(ach.getName())).onHover(TextActions.showAchievement(ach)).build());
		event.setMessageCancelled(
				handle(FormatTypes.ACHIEVEMENT, event, vars, event.getTargetEntity(), event.getChannel().get()));

	}

}