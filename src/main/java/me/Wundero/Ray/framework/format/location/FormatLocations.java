package me.Wundero.Ray.framework.format.location;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.boss.BossBarColor;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlay;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.ChatTypeMessageReceiver;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.title.Title;

import me.Wundero.Ray.Ray;
import me.Wundero.Ray.framework.channel.ChatChannel;
import me.Wundero.Ray.framework.format.Format;
import me.Wundero.Ray.framework.player.RayPlayer;
import me.Wundero.Ray.utils.TextUtils;
import me.Wundero.Ray.utils.Utils;
import ninja.leaping.configurate.ConfigurationNode;

/**
 * Singleton instances of locations. Proper descriptions on the Wiki.
 */
public class FormatLocations {

	/**
	 * Get a location from a string.
	 */
	public static FormatLocation fromString(String val) {
		val = val.toLowerCase().trim().replace("_", " ");
		switch (val) {
		case "scoreboard":
		case "s":
			return SCOREBOARD;
		case "title":
		case "t":
			return TITLE;
		case "subtitle":
		case "st":
			return SUBTITLE;
		case "actionbar":
		case "a":
			return ACTIONBAR;
		case "bossbar":
		case "b":
			return BOSSBAR;
		case "title & subtitle":
		case "ts":
		case "titlesubtitle":
		case "title subtitle":
			return TITLE_SUBTITLE;
		case "tab entry":
		case "te":
		case "tabentry":
		case "tabname":
		case "tab name":
		case "tablist entry":
		case "tablistentry":
		case "tablistname":
		case "tablist name":
		case "tn":
			return TAB_ENTRY;
		case "tab footer":
		case "tf":
		case "tablist footer":
			return TAB_FOOTER;
		case "tab header":
		case "th":
		case "tablist header":
			return TAB_HEADER;
		case "chat":
		case "c":
		default:
			return CHAT;
		}
	}

	/**
	 * Send a message to the chat. If chat menus are being used, try to put the
	 * message in a menu.
	 */
	public static final FormatLocation CHAT = new FormatLocation("chat") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> o, Optional<UUID> u,
				boolean broadcast) {
			if (Ray.get().isUseChatMenus() && target instanceof Player && o.isPresent() && u.isPresent()) {
				Object or = o.get();
				if (or instanceof CommandSource || or instanceof UUID) {
					RayPlayer r = RayPlayer.get((Player) target);
					if (or instanceof CommandSource) {
						if (or instanceof Player) {
							RayPlayer r2 = RayPlayer.get((Player) or);
							if (broadcast) {
								UUID r23 = UUID.randomUUID();
								List<ChatChannel> l1 = Ray.get().getChannels().getJoinableChannels((Player) target,
										false);
								List<ChatChannel> l2 = Ray.get().getChannels().getJoinableChannels((Player) or, false);
								Utils.intersect(l1, l2).stream()
										.filter(ch -> ch.getMenus().containsKey(((Player) target).getUniqueId())
												&& ch.getMenus().get(((Player) target).getUniqueId()) != null)
										.map(ch -> ch.getMenus().get(((Player) target).getUniqueId())).forEach(ch -> ch
												.addMessage((CommandSource) or, text, u.orElse(r23), !broadcast));
							} else if (r2.getActiveChannel().getMenus().containsKey(r.getUniqueId())
									&& r2.getActiveChannel().getMenus().get(r.getUniqueId()) != null) {
								r2.getActiveChannel().getMenus().get(r.getUniqueId()).addMessage((CommandSource) or,
										text, u.orElse(UUID.randomUUID()), !broadcast);
							} else {
								r.getActiveMenu().addMessage((CommandSource) or, text, u.orElse(UUID.randomUUID()),
										!broadcast);
							}
							return true;
						} else {
							if (broadcast) {
								UUID r23 = UUID.randomUUID();
								Ray.get().getChannels().getJoinableChannels((Player) target, false).stream()
										.filter(ch -> ch.getMenus().containsKey(((Player) target).getUniqueId())
												&& ch.getMenus().get(((Player) target).getUniqueId()) != null)
										.map(ch -> ch.getMenus().get(((Player) target).getUniqueId())).forEach(ch -> ch
												.addMessage((CommandSource) or, text, u.orElse(r23), !broadcast));
							} else {
								r.getActiveMenu().addMessage((CommandSource) or, text, u.orElse(UUID.randomUUID()),
										!broadcast);
							}
						}
						return true;
					} else {
						Optional<User> ou = Utils.getUser((UUID) or);
						if (ou.isPresent() && ou.get().getPlayer().isPresent() && ou.get().isOnline()) {
							Player pl = ou.get().getPlayer().get();
							RayPlayer r2 = RayPlayer.get(pl);
							if (broadcast) {
								UUID r23 = UUID.randomUUID();
								List<ChatChannel> l1 = Ray.get().getChannels().getJoinableChannels((Player) target,
										false);
								List<ChatChannel> l2 = Ray.get().getChannels().getJoinableChannels(pl, false);
								Utils.intersect(l1, l2).stream()
										.filter(ch -> ch.getMenus().containsKey(((Player) target).getUniqueId())
												&& ch.getMenus().get(((Player) target).getUniqueId()) != null)
										.map(ch -> ch.getMenus().get(((Player) target).getUniqueId()))
										.forEach(ch -> ch.addMessage(pl, text, u.orElse(r23), !broadcast));
							} else if (r2.getActiveChannel().getMenus().containsKey(r.getUniqueId())
									&& r2.getActiveChannel().getMenus().get(r.getUniqueId()) != null) {
								r2.getActiveChannel().getMenus().get(r.getUniqueId()).addMessage(pl, text,
										u.orElse(UUID.randomUUID()), !broadcast);
							} else {
								r.getActiveMenu().addMessage(pl, text, u.orElse(UUID.randomUUID()), !broadcast);
							}
							return true;
						}
					}
				} else {
					RayPlayer r = RayPlayer.get((Player) target);
					if (broadcast) {
						UUID r23 = UUID.randomUUID();
						Ray.get().getChannels().getJoinableChannels((Player) target, false).stream()
								.filter(ch -> ch.getMenus().containsKey(((Player) target).getUniqueId())
										&& ch.getMenus().get(((Player) target).getUniqueId()) != null)
								.map(ch -> ch.getMenus().get(((Player) target).getUniqueId()))
								.forEach(ch -> ch.addMessage((CommandSource) or, text, u.orElse(r23), !broadcast));
					} else {
						r.getActiveMenu().addMessage((CommandSource) or, text, u.orElse(UUID.randomUUID()), !broadcast);
					}
				}
			} else if (Ray.get().isUseChatMenus() && target instanceof Player) {
				RayPlayer r = RayPlayer.get((Player) target);
				if (broadcast) {
					UUID r23 = UUID.randomUUID();
					Ray.get().getChannels().getJoinableChannels((Player) target, false).stream()
							.filter(ch -> ch.getMenus().containsKey(((Player) target).getUniqueId())
									&& ch.getMenus().get(((Player) target).getUniqueId()) != null)
							.map(ch -> ch.getMenus().get(((Player) target).getUniqueId()))
							.forEach(ch -> ch.addMessage(null, text, u.orElse(r23), !broadcast));
				} else {
					r.getActiveMenu().addMessage(null, text, u.orElse(UUID.randomUUID()), !broadcast);
				}
			}
			target.sendMessage(text);
			return true;
		}
	};

	/**
	 * Send a text to the scoreboard.
	 */
	public static final FormatLocation SCOREBOARD = new FormatLocation("scoreboard") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> o, Optional<UUID> u,
				boolean broadcast) {
			// TODO this
			return false;
		}
	};

	/**
	 * Send a title/subtitle combo. NOTE: Requires '\n' to be present. If more
	 * than one is present, anything beyond and including the second one will be
	 * ignored.
	 */
	public static final FormatLocation TITLE_SUBTITLE = new FormatLocation("title_subtitle") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> x, Optional<UUID> u,
				boolean broadcast) {
			if (!(target instanceof Player)) {
				return false;
			}
			List<Text> tx = TextUtils.newlines(text);
			if (tx.size() < 2) {
				return false;
			}
			if (f == null || !f.getNode().isPresent()) {
				Player p = (Player) target;
				p.sendTitle(Title.builder().title(tx.get(0)).subtitle(tx.get(1)).fadeIn(0).stay(10).fadeOut(0).build());
				return true;
			}
			ConfigurationNode n = f.getNode().get().getNode("location-data");
			int i = n.getNode("fade-in").getInt(0);
			int s = n.getNode("fade-in").getInt(10);
			int o = n.getNode("fade-in").getInt(0);
			Player p = (Player) target;
			p.sendTitle(Title.builder().title(tx.get(0)).subtitle(tx.get(1)).fadeIn(i).stay(s).fadeOut(o).build());
			return true;
		}
	};

	/**
	 * Send a text as a subtitle.
	 */
	public static final FormatLocation SUBTITLE = new FormatLocation("subtitle") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> x, Optional<UUID> u,
				boolean broadcast) {
			if (!(target instanceof Player)) {
				return false;
			}
			if (f == null || !f.getNode().isPresent()) {
				Player p = (Player) target;
				p.sendTitle(Title.builder().subtitle(text).fadeIn(0).stay(10).fadeOut(0).build());
				return true;
			}
			ConfigurationNode n = f.getNode().get().getNode("location-data");
			int i = n.getNode("fade-in").getInt(0);
			int s = n.getNode("fade-in").getInt(10);
			int o = n.getNode("fade-in").getInt(0);
			Player p = (Player) target;
			p.sendTitle(Title.builder().subtitle(text).fadeIn(i).stay(s).fadeOut(o).build());
			return true;
		}
	};

	/**
	 * Send a text as a title.
	 */
	public static final FormatLocation TITLE = new FormatLocation("title") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> x, Optional<UUID> u,
				boolean broadcast) {
			if (!(target instanceof Player)) {
				return false;
			}
			if (f == null || !f.getNode().isPresent()) {
				Player p = (Player) target;
				p.sendTitle(Title.builder().title(text).fadeIn(0).stay(10).fadeOut(0).build());
				return true;
			}
			ConfigurationNode n = f.getNode().get().getNode("location-data");
			int i = n.getNode("fade-in").getInt(0);
			int s = n.getNode("fade-in").getInt(10);
			int o = n.getNode("fade-in").getInt(0);
			Player p = (Player) target;
			p.sendTitle(Title.builder().title(text).fadeIn(i).stay(s).fadeOut(o).build());
			return true;
		}
	};

	/**
	 * Send a text to the bossbar.
	 */
	public static final FormatLocation BOSSBAR = new FormatLocation("bossbar") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> o, Optional<UUID> u,
				boolean broadcast) {
			if (!(target instanceof Player)) {
				return false;
			}
			if (f == null || !f.getNode().isPresent()) {
				return false;
			}
			ConfigurationNode n = f.getNode().get().getNode("location-data");
			String col = n.getNode("color").getString("white");
			double per = n.getNode("percent").getDouble(1);
			int notchScale = n.getNode("notches").getInt(0);
			BossBarColor c = BossBarColors.WHITE;
			switch (col.toLowerCase().trim()) {
			case "blue":
				c = BossBarColors.BLUE;
				break;
			case "pink":
				c = BossBarColors.PINK;
				break;
			case "yellow":
				c = BossBarColors.YELLOW;
				break;
			case "green":
				c = BossBarColors.GREEN;
				break;
			case "red":
				c = BossBarColors.RED;
				break;
			case "purple":
				c = BossBarColors.PURPLE;
				break;
			default:
				c = BossBarColors.WHITE;
				break;

			}
			BossBarOverlay ov = BossBarOverlays.PROGRESS;
			switch (notchScale) {
			case 1:
			case 6:
				ov = BossBarOverlays.NOTCHED_6;
				break;
			case 10:
			case 2:
				ov = BossBarOverlays.NOTCHED_10;
				break;
			case 12:
			case 3:
				ov = BossBarOverlays.NOTCHED_12;
				break;
			case 20:
			case 4:
				ov = BossBarOverlays.NOTCHED_20;
				break;
			case 0:
			default:
				ov = BossBarOverlays.PROGRESS;
				break;
			}
			ServerBossBar.builder().name(text).color(c).createFog(false).darkenSky(false).playEndBossMusic(false)
					.visible(true).overlay(ov).percent((float) per).build().addPlayer((Player) target);
			return true;
		}
	};

	/**
	 * Send a text to the actionbar.
	 */
	public static final FormatLocation ACTIONBAR = new FormatLocation("actionbar") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> operatable, Optional<UUID> u,
				boolean broadcast) {
			if (!(target instanceof ChatTypeMessageReceiver)) {
				return false;
			}
			ChatTypeMessageReceiver r = (ChatTypeMessageReceiver) target;
			r.sendMessage(ChatTypes.ACTION_BAR, text);
			return true;
		}
	};

	/**
	 * Send a text as a player name. Requires Optional<Object> to be present.
	 */
	public static final FormatLocation TAB_ENTRY = new FormatLocation("tab_entry") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> p, Optional<UUID> u,
				boolean broadcast) {
			if (p == null || !p.isPresent() || !(target instanceof Player)) {
				return false;
			}
			Object operatable = p.get();
			UUID uuid;
			if (operatable instanceof UUID) {
				uuid = (UUID) operatable;
			} else if (operatable instanceof User) {
				uuid = ((User) operatable).getUniqueId();
			} else {
				return false;
			}
			Optional<TabListEntry> oe = ((Player) target).getTabList().getEntry(uuid);
			oe.ifPresent(e -> e.setDisplayName(text));
			return oe.isPresent();
		}
	};

	/**
	 * Send a text to the footer.
	 */
	public static final FormatLocation TAB_FOOTER = new FormatLocation("tab_footer") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> o, Optional<UUID> u,
				boolean broadcast) {
			if (!(target instanceof Player)) {
				return false;
			}
			Player p = (Player) target;
			RayPlayer.get(p.getUniqueId()).queueFooter(text);
			return true;
		}
	};

	/**
	 * Send a text to the header.
	 */
	public static final FormatLocation TAB_HEADER = new FormatLocation("tab_header") {
		@Override
		public boolean send(Text text, MessageReceiver target, Format f, Optional<Object> o, Optional<UUID> u,
				boolean broadcast) {
			if (!(target instanceof Player)) {
				return false;
			}
			Player p = (Player) target;
			RayPlayer.get(p.getUniqueId()).queueHeader(text);
			return true;
		}
	};

}
