package me.Wundero.Ray.framework.player;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.Validate;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.tab.TabList;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import com.google.common.reflect.TypeToken;

import me.Wundero.Ray.Ray;
import me.Wundero.Ray.animation.Animation;
import me.Wundero.Ray.animation.AnimationQueue;
import me.Wundero.Ray.framework.Group;
import me.Wundero.Ray.framework.channel.ChatChannel;
import me.Wundero.Ray.framework.format.location.FormatLocation;
import me.Wundero.Ray.tag.SelectableTag;
import me.Wundero.Ray.utils.Utils;
import me.Wundero.Ray.variables.ParsableData;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

/**
 * Player wrapper for the plugin. Holds information the plugin needs about the
 * player in memory, and saves some to disk.
 */
public class RayPlayer implements Socialable {

	private static Map<UUID, RayPlayer> cache = Utils.sm();

	/**
	 * Get the RayPlayer representing the player with UUID @param u
	 */
	public static RayPlayer getRay(UUID u) {
		if (!cache.containsKey(u)) {
			Optional<Player> p = Sponge.getServer().getPlayer(u);
			if (!p.isPresent()) {
				UserStorageService storage = Ray.get().getPlugin().getGame().getServiceManager()
						.provide(UserStorageService.class).get();
				Optional<User> opt = storage.get(u);
				if (opt.isPresent()) {
					return new RayPlayer(opt.get());
				}
				return null;
			}
			Player u2 = p.get();
			return new RayPlayer(u2);
		} else
			return cache.get(u);
	}

	/**
	 * Get the RayPlayer representing the User @param u
	 */
	public static RayPlayer getRay(User u) {
		if (!cache.containsKey(u.getUniqueId())) {
			return new RayPlayer(u);
		}
		return cache.get(u.getUniqueId());
	}

	/**
	 * Run a tablist update (names only)
	 */
	public static void updateTabs() {
		for (RayPlayer p : cache.values()) {
			if (p.isOnline()) {
				p.updateTab();
			}
		}
	}

	/**
	 * Update player names in the tablist
	 */
	public void updateTab() {
		Task.builder().execute(tabTask).submit(Ray.get().getPlugin());
	}

	/**
	 * Save all player data to disk
	 */
	public static void saveAll() throws ObjectMappingException {
		for (RayPlayer p : cache.values()) {
			p.save();
		}
	}

	/**
	 * Get the RayPlayer representing the player with UUID @param u
	 */
	public static RayPlayer get(UUID u) {
		return getRay(u);
	}

	/**
	 * Get the RayPlayer representing the User @param u
	 */
	public static RayPlayer get(User u) {
		return getRay(u);
	}

	private UUID uuid;
	private Map<String, Group> groups;
	private Optional<RayPlayer> lastMessaged = Optional.empty();
	private List<UUID> ignore = Utils.sl();
	private ConfigurationNode config;
	private ChatChannel activeChannel = null;
	private Runnable tabTask;
	private boolean afk = false;
	private Task tabHFTask = null;
	private ArrayDeque<Text> headerQueue = new ArrayDeque<>(), footerQueue = new ArrayDeque<>();
	private List<String> listenChannels = Utils.sl();
	private Map<FormatLocation, AnimationQueue> animations = Utils.sm();
	private Map<SelectableTag, String> selectedTags = Utils.sm();
	private boolean spy = false;
	private Optional<String> quote;

	/**
	 * Get the player object if user is online.
	 */
	public Optional<Player> getPlayer() {
		return getUser().getPlayer();
	}

	/**
	 * Check to see if the player is online.
	 */
	public boolean isOnline() {
		User u = getUser();
		return u.isOnline() && u.getPlayer().isPresent();
	}

	/**
	 * Check to see if the player has a permission.
	 */
	public boolean hasPermission(String s) {
		return getUser().hasPermission(s);
	}

	/**
	 * @return whether the player is AFK.
	 */
	public boolean AFK() {
		return afk;
	}

	/**
	 * Toggle whether the player is AFK.
	 */
	public void toggleAFK() {
		this.afk = !afk;
	}

	/**
	 * Set whether the player is AFK.
	 */
	public void setAFK(boolean a) {
		this.afk = a;
	}

	/**
	 * Set the player's quote
	 */
	public void setQuote(String quote) {
		setQuote(Utils.wrap(quote, !quote.trim().isEmpty()));
	}

	/**
	 * Set the player's quote
	 */
	public void setQuote(Optional<String> quote) {
		// Empty string safeguard
		setQuote(quote.orElse(null));
	}

	/**
	 * Get the player's quote
	 */
	public Optional<String> getQuote() {
		return quote;
	}

	/**
	 * @return whether the player is spying on messages
	 */
	public boolean spy() {
		return spy;
	}

	/**
	 * Set the player's spy toggle
	 */
	public void setSpy(boolean spy) {
		if (hasPermission("ray.spy") && spy) {
			this.spy = spy;
		} else {
			this.spy = false;
		}
	}

	/**
	 * Choose a subtag for a tag
	 */
	public void select(SelectableTag tag, String sub) {
		this.selectedTags.put(tag, sub);
	}

	/**
	 * Get the subtag chosen for a tag
	 */
	public Optional<String> getSelected(SelectableTag tag) {
		if (tag == null) {
			return Optional.empty();
		}
		return Utils.wrap(selectedTags.get(tag));
	}

	/**
	 * Get the subtag chosen for a tag with the name specified.
	 */
	public Optional<String> getSelected(String name) {
		if (name == null) {
			return Optional.empty();
		}
		if (Ray.get().getTags().get(name, Utils.sm(), SelectableTag.class).isPresent()) {
			return Utils.wrap(selectedTags.get(Ray.get().getTags().get(name, Utils.sm(), SelectableTag.class).get()));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Queue an animation in a certain location
	 */
	public void queueAnimation(FormatLocation type, Animation<?> anim) {
		if (!animations.containsKey(type)) {
			animations.put(type, new AnimationQueue());
		}
		animations.get(type).queueAnimation(anim);
	}

	/**
	 * Queue a footer text
	 */
	public synchronized void queueFooter(Text t) {
		if (t != null) {
			synchronized (footerQueue) {
				footerQueue.add(t);
			}
		}
	}

	/**
	 * Queue a header text
	 */
	public synchronized void queueHeader(Text t) {
		if (t != null) {
			synchronized (headerQueue) {
				headerQueue.add(t);
			}
		}
	}

	/**
	 * Stop the header footer update task
	 */
	public void stopTabHFTask() {
		tabHFTask.cancel();
	}

	private synchronized Text pop(ArrayDeque<Text> from, Text def) {
		return Utils.pop(from, def);
	}

	private static final Text EMPTY_TEXT_HEADER = TextSerializers.JSON.deserialize("{\"translate\":\"\"}");

	/**
	 * Start the header footer update task
	 */
	public void startTabHFTask() {
		tabHFTask = Task.builder().execute(t -> {
			if (!isOnline()) {
				return;
			}
			Player p = getPlayer().get();
			TabList list = p.getTabList();
			boolean h = !headerQueue.isEmpty() || !list.getHeader().isPresent();
			boolean f = !footerQueue.isEmpty() || !list.getFooter().isPresent();
			if (h && f) {
				synchronized (headerQueue) {
					synchronized (footerQueue) {
						Text he = pop(headerQueue, EMPTY_TEXT_HEADER);
						Text fo = pop(footerQueue, EMPTY_TEXT_HEADER);
						p.getTabList().setHeaderAndFooter(he, fo);
					}
				}
			} else if (h) {
				synchronized (headerQueue) {
					Text he = pop(headerQueue, EMPTY_TEXT_HEADER);
					p.getTabList().setHeader(he);
				}
			} else if (f) {
				synchronized (footerQueue) {
					Text he = pop(footerQueue, EMPTY_TEXT_HEADER);
					p.getTabList().setFooter(he);
				}
			}
		}).intervalTicks(1).submit(Ray.get().getPlugin());
	}

	/**
	 * Check to see if the player is reading messages from a channel
	 */
	public boolean listeningTo(ChatChannel c) {
		return listeningTo(c.getName());
	}

	/**
	 * Check to see if the player is reading messages from a channel
	 */
	public boolean listeningTo(String channel) {
		return listenChannels.contains(channel);
	}

	/**
	 * Stop reading messages from a channel
	 */
	public void removeListenChannel(ChatChannel c) {
		removeListenChannel(c.getName());
	}

	/**
	 * Stop reading messages from a channel
	 */
	public boolean removeListenChannel(String s) {
		if (!listenChannels.contains(s)) {
			return false;
		}
		return listenChannels.remove(s);
	}

	/**
	 * Start reading messages from a channel
	 */
	public void addListenChannel(ChatChannel c) {
		listenChannels.add(c.getName());
	}

	/**
	 * @return whether the @param player is being ignored
	 */
	public boolean isIgnoring(RayPlayer player) {
		return ignore.contains(player.uuid);
	}

	/**
	 * Toggle whether the player is being ignored
	 */
	public boolean toggleIgnore(RayPlayer player) {
		if (isIgnoring(player)) {
			return unignore(player);
		} else {
			return ignore(player);
		}
	}

	/**
	 * Stop ignoring the player
	 */
	public boolean unignore(RayPlayer player) {
		return ignore.remove(player.uuid);
	}

	/**
	 * Start ignoring the player
	 */
	public boolean ignore(RayPlayer player) {
		if (ignore.contains(player.uuid)) {
			return false;
		}
		return ignore.add(player.uuid);
	}

	/**
	 * @return the channel this player speaks into
	 */
	public ChatChannel getActiveChannel() {
		return activeChannel;
	}

	/**
	 * Set the active chanel as the messagechannel of the player
	 */
	public void applyChannel() {
		if (activeChannel != null) {
			if (isOnline()) {
				getPlayer().get().setMessageChannel(activeChannel);
			}
		}
	}

	/**
	 * Set the channel this user speaks into
	 */
	public void setActiveChannel(ChatChannel channel) {
		if (channel == null) {
			return;
		}
		activeChannel = channel;
		if (isOnline()) {
			getPlayer().get().setMessageChannel(activeChannel);
		}
	}

	/**
	 * Load player data from file
	 */
	public void load() throws ObjectMappingException {
		if (config == null) {
			return;
		}
		ConfigurationNode i = config.getNode("ignoring");
		ignore = Utils.sl(i.getList(TypeToken.of(UUID.class)), true);
		setActiveChannel(Ray.get().getChannels().getChannel(config.getNode("channel").getString()));
		this.spy = config.getNode("spy").getBoolean(false);
		loadTags(config.getNode("tags"));
		this.quote = Utils.wrap(config.getNode("quote").getString());
	}

	private static Map<SelectableTag, String> deconvert(Map<String, String> in) {
		Map<SelectableTag, String> out = Utils.sm();
		if (in != null) {
			for (String t : in.keySet()) {
				out.put(Ray.get().getTags().get(t, Utils.sm(), SelectableTag.class).get(), out.get(t));
			}
		}
		return out;
	}

	private static Map<String, String> convert(Map<SelectableTag, String> in) {
		Map<String, String> out = Utils.sm();
		for (SelectableTag t : in.keySet()) {
			out.put(t.getName(), in.get(t));
		}
		return out;
	}

	/**
	 * Save player data to file
	 */
	public void save() throws ObjectMappingException {
		if (config == null) {
			return;
		}
		config.getNode("ignoring").setValue(ignore);
		config.getNode("channel")
				.setValue(activeChannel == null ? config.getNode("channel").getString(null) : activeChannel.getName());
		saveTags(config.getNode("tags"));
		config.getNode("lastname").setValue(getUser().getName());
		if (getDisplayName().isPresent()) {
			config.getNode("displayname").setValue(TypeToken.of(Text.class), getDisplayName().get());
		}
		config.getNode("spy").setValue(spy);
		quote.ifPresent(q -> config.getNode("quote").setValue(q));
	}

	private void saveTags(ConfigurationNode node) {
		Map<String, String> con = convert(selectedTags);
		for (Map.Entry<String, String> e : con.entrySet()) {
			node.getNode(e.getKey()).setValue(e.getValue());
		}
	}

	private void loadTags(ConfigurationNode node) {
		Map<String, String> o = Utils.sm();
		for (Entry<Object, ? extends ConfigurationNode> e : node.getChildrenMap().entrySet()) {
			o.put(e.getKey().toString(), e.getValue().getString());
		}
		this.selectedTags = deconvert(o);
	}

	/**
	 * Return the textual displayname of the player
	 */
	public Optional<Text> getDisplayName() {
		return Optional.ofNullable(displayname);
	}

	private Text displayname = null;

	/**
	 * Set displayname if the player is online
	 */
	public void checkDisplayname() {
		if (!isOnline()) {
			return;
		}
		Object o = Ray.get().getVariables().get("displayname",
				new ParsableData().withSender(this.getUser().getPlayer().get()), Optional.empty(), Optional.empty());
		displayname = o instanceof Text ? (Text) o : Text.of(o);
	}

	/**
	 * Wrap the user in the RayPlayer data holder
	 */
	public RayPlayer(User u) {
		this.setUser(u);
		this.uuid = u.getUniqueId();
		this.setGroups(Ray.get().getGroups().getGroups(u));
		this.checkDisplayname();
		File p = new File(Ray.get().getPlugin().getConfigDir().toFile(), "players");
		File f = new File(p, u.getUniqueId() + ".conf");
		if (!p.exists()) {
			p.mkdirs();
		}
		if (f.exists()) {
			config = Utils.load(f);
			try {
				load();
			} catch (Exception e) {
				Utils.printError(e);
			}
		} else {
			try {
				f.createNewFile();
			} catch (IOException e) {
				Utils.printError(e);
			}
			config = Utils.load(f);
		}
		cache.put(uuid, this);
	}

	/**
	 * Get the user's UUID
	 */
	public UUID getUniqueId() {
		return getUUID();
	}

	/**
	 * Get the user's UUID
	 */
	private UUID getUUID() {
		return uuid;
	}

	/**
	 * @return the user
	 */
	public User getUser() {
		return Utils.getUser(uuid).get();
	}

	/**
	 * Set the user
	 */
	private void setUser(User user) {
		this.uuid = user.getUniqueId();
	}

	/**
	 * @return the groups this user is in
	 */
	public Map<String, Group> getGroups() {
		return groups;
	}

	/**
	 * Return the group applied for the user's current world
	 */
	public Group getActiveGroup() {
		if (!getUser().isOnline()) {
			return gg("all");
		}
		return gg((getPlayer().get()).getWorld().getName()) == null ? gg("all")
				: gg((getPlayer().get()).getWorld().getName());
	}

	private Group gg(String world) {
		return getGroups().get(world);
	}

	/**
	 * Set the user's groups
	 */
	public void setGroups(Map<String, Group> groups) {
		this.groups = groups;
	}

	/**
	 * Reload the player's active groups
	 */
	public void reloadGroups() {
		this.setGroups(Ray.get().getGroups().getGroups(this.getUser()));
	}

	/**
	 * Get the player this user sent a private message to most recently.
	 */
	public Optional<RayPlayer> getLastMessaged() {
		return lastMessaged;
	}

	/**
	 * Set the player this user sent a private message to most recently.
	 */
	public void setLastMessaged(Optional<RayPlayer> lastMessaged, boolean recurse) {
		if (!lastMessaged.isPresent()) {
			return;
		}
		if (recurse) {
			lastMessaged.get().setLastMessaged(Optional.of(this), false);
		}
		this.lastMessaged = lastMessaged;
	}

	/**
	 * Get the player data file
	 */
	public ConfigurationNode getConfig() {
		return config;
	}

	/**
	 * Set the player data file
	 */
	public void setConfig(ConfigurationNode config) {
		this.config = config;
	}

	/**
	 * Get the task executed when tablist updates are called
	 */
	public Runnable getTabTask() {
		return tabTask;
	}

	/**
	 * Set the task executed when tablist updates are called
	 */
	public void setTabTask(Runnable tabTask) {
		this.tabTask = tabTask;
	}

	/**
	 * Get all channels this user receives messages from
	 */
	public List<String> getListenChannels() {
		return listenChannels;
	}

	/**
	 * Set all channels this user receives messages from
	 */
	public void setListenChannels(List<String> listenChannels) {
		this.listenChannels = listenChannels;
	}

	private Map<SocialMedia, String> mediums = Utils.sm();

	public void setMediaName(String name, SocialMedia medium) {
		Validate.notNull(name);
		Validate.isTrue(!name.isEmpty());
		this.mediums.put(medium, name);
	}

	/**
	 * Get the URL to link to this player's social media accounts
	 */
	@Override
	public URL getMediaURL(SocialMedia medium) {
		if (medium == null || !mediums.containsKey(medium)) {
			return null;
		}
		String name = mediums.get(medium);
		if (name != null) {
			return medium.apply(name);
		}
		return null;
	}

}
