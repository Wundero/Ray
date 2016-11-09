package me.Wundero.Ray.framework.channel;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.AbstractMutableMessageChannel;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.serializer.TextSerializers;

import com.google.common.reflect.TypeToken;

import me.Wundero.Ray.utils.TextUtils;
import me.Wundero.Ray.utils.Utils;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

/**
 * Represents a channel through which users communicate.
 */
public class ChatChannel extends AbstractMutableMessageChannel implements Comparable<ChatChannel> {

	private List<UUID> muted, banned = Utils.sl();
	private Map<UUID, Role> roles = Utils.sm();;
	private String name;
	private String permission;
	private Optional<String> password;
	private Text tag;
	private double range;
	private boolean hidden = false;
	private boolean autojoin = true;
	private boolean obfuscateRanged = false;
	private ConfigurationNode node;
	private Role defRole = Role.GUEST;

	/**
	 * Set the role of a player.
	 */
	public boolean setRole(MessageReceiver r, Role role) {
		if (r instanceof Player && hasMember(r)) {
			return setRole(((Player) r).getUniqueId(), role);
		}
		return false;
	}

	/**
	 * Set the role of a player.
	 */
	public boolean setRole(UUID u, Role r) {
		if (!hasMember(u) || r == null) {
			return false;
		}
		roles.put(u, r);
		return true;
	}

	/**
	 * Get the role of a player.
	 */
	public Role getRole(MessageReceiver r) {
		if (!hasMember(r)) {
			return null;
		}
		if (r instanceof Player) {
			return roles.get(((Player) r).getUniqueId());
		} else {
			return Role.MOD;
		}
	}

	/**
	 * Get the role of the player.
	 */
	public Role getRole(UUID u) {
		if (!hasMember(u)) {
			return null;
		}
		return roles.get(u);
	}

	/**
	 * Return the size of the array.
	 */
	public int getSize() {
		return (int) members.stream().filter(recip -> recip instanceof Player).map((r) -> ((Player) r).getUniqueId())
				.distinct().count();
	}

	public boolean removeMember(UUID u) {
		Optional<User> ou = Utils.getUser(u);
		if (!ou.isPresent()) {
			return false;
		}
		User uu = ou.get();
		if (uu.isOnline() && uu.getPlayer().isPresent()) {
			return removeMember(uu.getPlayer().get());
		}
		return false;
	}

	public boolean addMember(MessageReceiver member, Optional<String> password) {
		if (!this.password.isPresent()) {
			return addMember(member);
		} else {
			if (password.isPresent()) {
				if (password.get().equals(this.password.get())) {
					return addMember(member);
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}

	@Deprecated
	@Override
	public boolean addMember(MessageReceiver member) {
		if (member instanceof Player) {
			UUID u = ((Player) member).getUniqueId();
			if (banned.contains(u)) {
				return false;
			}
			if (!roles.containsKey(u)) {
				roles.put(u, defRole);
			}
		}
		return super.addMember(member);
	}

	/**
	 * Type serializers for easy configuration saving.
	 */
	public static TypeSerializer<ChatChannel> serializer() {
		return new TypeSerializer<ChatChannel>() {

			@Override
			public ChatChannel deserialize(TypeToken<?> arg0, ConfigurationNode arg1) throws ObjectMappingException {
				ChatChannel out = new ChatChannel();
				out.name = arg1.getNode("name").getString();
				out.permission = arg1.getNode("permission").getString(null);
				out.tag = TextSerializers.FORMATTING_CODE
						.deserialize(arg1.getNode("tag").getString("[" + out.name.charAt(0) + "]"));
				out.range = arg1.getNode("range").getDouble(-1);
				out.hidden = arg1.getNode("hidden").getBoolean(false);
				out.autojoin = arg1.getNode("autojoin").getBoolean(true);
				out.password = Utils.wrap2(arg1.getNode("password").getString(), (s) -> {
					return s.isPresent() && !s.get().trim().isEmpty();
				});
				out.obfuscateRanged = arg1.getNode("range-obfuscation").getBoolean(false);
				out.node = arg1;
				out = deserializeMembers(out, arg1);
				return out;
			}

			@Override
			public void serialize(TypeToken<?> arg0, ChatChannel arg1, ConfigurationNode arg2)
					throws ObjectMappingException {
				arg2.getNode("name").setValue(arg1.name);
				if (arg1.permission != null && !arg1.permission.isEmpty()) {
					arg2.getNode("permission").setValue(arg1.permission);
				}
				arg2.getNode("tag").setValue(TextSerializers.FORMATTING_CODE.serialize(arg1.tag));
				arg2.getNode("range").setValue(arg1.range);
				arg2.getNode("hidden").setValue(arg1.hidden);
				arg2.getNode("autojoin").setValue(arg1.autojoin);
				arg1.password.ifPresent(pass -> arg2.getNode("password").setValue(pass));
				arg2.getNode("range-obfuscation").setValue(arg1.obfuscateRanged);
				serializeMembers(arg1, arg2);
			}
		};
	}

	private static ChatChannel deserializeMembers(ChatChannel ch, ConfigurationNode node) {
		ConfigurationNode n = node.getNode("members");
		for (ConfigurationNode user : n.getChildrenMap().values()) {
			UUID u = UUID.fromString(user.getKey().toString());
			boolean m = user.getNode("muted").getBoolean(false);
			boolean b = user.getNode("banned").getBoolean(false);
			Role r = Role.valueOf(user.getNode("role").getString(ch.defRole.name()).toUpperCase());
			ch.roles.put(u, r);
			if (m) {
				ch.muted.add(u);
			}
			if (b) {
				ch.banned.add(u);
			}
		}
		return ch;
	}

	private static void serializeMembers(ChatChannel ch, ConfigurationNode node) {
		ConfigurationNode n = node.getNode("members");
		for (UUID user : ch.roles.keySet()) {
			ConfigurationNode c = n.getNode(user.toString());
			c.getNode("muted").setValue(ch.muted.contains(user));
			c.getNode("banned").setValue(ch.banned.contains(user));
			c.getNode("role").setValue(ch.roles.get(user).name().toLowerCase());
		}
	}

	/**
	 * Returns whether or not the receiver can speak into the channel.
	 */
	public boolean canSpeak(MessageReceiver r) {
		if (r instanceof Player) {
			return canSpeak(((Player) r).getUniqueId());
		} else {
			return members.contains(r);
		}
	}

	/**
	 * Returns whether or not the player with uuid u can speak.
	 */
	public boolean canSpeak(UUID u) {
		if (banned.contains(u) || muted.contains(u)) {
			return false;
		}
		User ud = Utils.getUser(u).get();
		return ud.hasPermission(this.permission) && members.contains(ud);
	}

	/**
	 * Check to see whether the member is in the channel.
	 */
	public boolean hasMember(MessageReceiver r) {
		if (r instanceof Player) {
			return hasMember(((Player) r).getUniqueId()) && members.contains(r);
		} else {
			return members.contains(r);
		}
	}

	/**
	 * Check to see whether the member is in the channel.
	 */
	public boolean hasMember(UUID u) {
		return roles.containsKey(u);
	}

	/**
	 * Check to see whether the receiver can join the channel.
	 */
	public boolean canJoin(MessageReceiver r, Optional<String> password) {
		if (r instanceof Player) {
			return canJoin(((Player) r).getUniqueId(), password);
		} else {
			return !members.contains(r);
		}
	}

	/**
	 * Check to see whether the player can join the channel.
	 */
	public boolean canJoin(UUID uuid, Optional<String> password) {
		if (banned.contains(uuid)) {
			return false;
		}
		if (this.password.isPresent()) {
			if (!password.isPresent() || !password.get().equals(this.password.get())) {
				return false;
			}
		}
		User u = Utils.getUser(uuid).get();
		if (u.hasPermission(this.permission)) {
			return !members.contains(u);
		}
		return false;
	}

	/**
	 * Toggle whether a player is banned.
	 */
	public boolean toggleBan(MessageReceiver r) {
		return r instanceof Player ? toggleBan(((Player) r).getUniqueId()) : false;
	}

	/**
	 * Toggle whether a player is banned.
	 */
	public boolean toggleBan(UUID u) {
		if (isBanned(u)) {
			return unban(u);
		} else {
			return ban(u);
		}
	}

	/**
	 * Ban a player.
	 */
	public boolean ban(MessageReceiver r) {
		return r instanceof Player ? ban(((Player) r).getUniqueId()) : false;
	}

	/**
	 * Ban a player.
	 */
	public boolean ban(UUID u) {
		if (banned.contains(u)) {
			return false;
		}
		return banned.add(u);
	}

	/**
	 * Unban a player.
	 */
	public boolean unban(MessageReceiver r) {
		return r instanceof Player ? unban(((Player) r).getUniqueId()) : false;
	}

	/**
	 * Unban a player.
	 */
	public boolean unban(UUID u) {
		return banned.remove(u);
	}

	/**
	 * Toggle whether a player is muted.
	 */
	public boolean toggleMute(MessageReceiver r) {
		return r instanceof Player ? toggleMute(((Player) r).getUniqueId()) : false;
	}

	/**
	 * Toggle whether a player is muted.
	 */
	public boolean toggleMute(UUID u) {
		if (isMuted(u)) {
			return unmute(u);
		} else {
			return mute(u);
		}
	}

	/**
	 * Mute a player.
	 */
	public boolean mute(MessageReceiver r) {
		return r instanceof Player ? mute(((Player) r).getUniqueId()) : false;
	}

	/**
	 * Mute a player.
	 */
	public boolean mute(UUID u) {
		if (muted.contains(u)) {
			return false;
		}
		return muted.add(u);
	}

	/**
	 * Unmute a player.
	 */
	public boolean unmute(MessageReceiver r) {
		return r instanceof Player ? unmute(((Player) r).getUniqueId()) : false;
	}

	/**
	 * Unmute a player.
	 */
	public boolean unmute(UUID u) {
		return muted.remove(u);
	}

	/**
	 * Check to see whether a player is muted.
	 */
	public boolean isMuted(MessageReceiver r) {
		if (r instanceof Player) {
			return isMuted(((Player) r).getUniqueId());
		}
		return false;
	}

	/**
	 * Check to see whether a player is muted.
	 */
	public boolean isMuted(UUID u) {
		return muted.contains(u);
	}

	/**
	 * Check to see whether a player is banned.
	 */
	public boolean isBanned(UUID u) {
		return banned.contains(u);
	}

	/**
	 * Check to see whether a player is banned.
	 */
	public boolean isBanned(MessageReceiver r) {
		if (r instanceof Player) {
			return isBanned(((Player) r).getUniqueId());
		}
		return false;
	}

	/**
	 * Create a new chat channel that sends to console only.
	 */
	public ChatChannel() {
		members.addAll(MessageChannel.TO_CONSOLE.getMembers());
	}

	/**
	 * Make sure the message can be sent; obfuscates it as well (obfuscation is
	 * overwritten by MainListener.handle() if that is called.
	 */
	@Override
	public Optional<Text> transformMessage(Object sender, MessageReceiver recipient, Text original, ChatType type) {
		if (!hasMember(recipient) || isBanned(recipient)) {
			// if recip is not in or is banned
			return Optional.empty();
		}
		if (sender instanceof MessageReceiver
				&& (!hasMember((MessageReceiver) sender) || !canSpeak((MessageReceiver) sender))) {
			// if sender is not in, is banned or is muted
			return Optional.empty();
		}
		if (range > 0 && range < Double.MAX_VALUE && sender instanceof Player && recipient instanceof Player) {
			Player p = (Player) sender;
			Player r = (Player) recipient;
			boolean ir = Utils.inRange(p.getLocation(), r.getLocation(), range);
			double delta = Utils.difference(p.getLocation(), r.getLocation());
			if (!ir && !this.isObfuscateRanged()) {
				return Optional.empty();
			} else if (!ir) {
				double percentObfuscation = ((delta - range) / (delta * (2 * (range / delta)))) * 100;
				double percentDiscoloration = ((delta - range) / delta) * 100;
				return Optional.of(TextUtils.obfuscate(original, percentObfuscation, percentDiscoloration));
			}
		}
		return Optional.of(original);
	}

	/**
	 * @return the range.
	 */
	public double range() {
		return range;
	}

	/**
	 * @return the display tag of the channel.
	 */
	public Text getTag() {
		return tag;
	}

	/**
	 * @return the name of the channel.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the permission the channel uses.
	 */
	public String getPermission() {
		return permission;
	}

	/**
	 * @return whether the channel shows up when /ch is typed.
	 */
	public boolean isHidden() {
		return hidden;
	}

	/**
	 * set whether the channel is hidden.
	 */
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * @return the config node.
	 */
	public ConfigurationNode getNode() {
		return node;
	}

	/**
	 * @return whether players can automatically join the channel.
	 */
	public boolean isAutojoin() {
		return autojoin;
	}

	/**
	 * set whether players can autojoin.
	 */
	public void setAutojoin(boolean autojoin) {
		this.autojoin = autojoin;
	}

	@Override
	public int compareTo(ChatChannel o) {
		int i = members.size() - o.members.size();
		if (i == 0) {
			if (range <= 0) {
				if (o.range > 0) {
					return (int) o.range;
				} else {
					return 0;
				}
			} else {
				if (o.range <= 0) {
					return (int) -range;
				} else {
					return (int) (range - o.range);
				}
			}
		}
		return i;
	}

	/**
	 * @return the obfuscateRanged
	 */
	public boolean isObfuscateRanged() {
		return obfuscateRanged;
	}

	/**
	 * @param obfuscateRanged
	 *            the obfuscateRanged to set
	 */
	public void setObfuscateRanged(boolean obfuscateRanged) {
		this.obfuscateRanged = obfuscateRanged;
	}

	/**
	 * @param text
	 *            the password to set
	 */
	public void setPassword(String text) {
		password = Utils.wrap2(text, (s) -> {
			return s.isPresent() && !s.get().trim().isEmpty();
		});
	}

	/**
	 * @return the password
	 */
	public Optional<String> getPassword() {
		return password;
	}

}
