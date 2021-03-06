package me.Wundero.Ray.framework.format;
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

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.channel.MessageReceiver;

import me.Wundero.Ray.Ray;
import me.Wundero.Ray.conversation.ConversationContext;
import me.Wundero.Ray.conversation.Option;
import me.Wundero.Ray.conversation.Prompt;
import me.Wundero.Ray.utils.EventUtils;
import me.Wundero.Ray.utils.Utils;
import me.Wundero.Ray.variables.ParsableData;
import ninja.leaping.configurate.ConfigurationNode;

/**
 * Format type that is fired when an event, which is a subclass of or equal to
 * the provided event class, is called.
 * 
 * TODO more variables in onEvent
 */
public class EventFormat extends Format {

	private static Map<String, Class<?>> spongeEvents = Utils.sm();
	static {
		spongeEvents.put("join", ClientConnectionEvent.Join.class);
		spongeEvents.put("login", ClientConnectionEvent.Join.class);
		spongeEvents.put("leave", ClientConnectionEvent.Disconnect.class);
		spongeEvents.put("disconnect", ClientConnectionEvent.Disconnect.class);
		spongeEvents.put("quit", ClientConnectionEvent.Disconnect.class);
		spongeEvents.put("chat", MessageChannelEvent.Chat.class);
	}

	private Optional<String> eventClass = Optional.empty();

	private Format internal;
	private boolean cancel;

	/**
	 * Create a new event format.
	 */
	public EventFormat(ConfigurationNode node, Format f) {
		super(node);
		if (node == null || node.isVirtual()) {
			return;
		}
		internal = f;
		String cname = node.getNode("event").getString();
		this.eventClass = Utils.wrap(cname, !cname.trim().isEmpty());
		this.cancel = node.getNode("cancel").getBoolean(true);
		Task.builder().intervalTicks(20).execute((task) -> {
			if (f.usable) {
				Sponge.getEventManager().registerListeners(Ray.get().getPlugin(), this);
				task.cancel();
			}
		}).submit(Ray.get().getPlugin());
	}

	private boolean needsDelay(Class<?> clazz) {
		return ClientConnectionEvent.Join.class.isAssignableFrom(clazz);
	}

	private boolean checkClass(Class<?> clazz) {
		if (!eventClass.isPresent()) {
			return false;
		}
		String ec = eventClass.get().toLowerCase().trim();
		if (spongeEvents.containsKey(ec) && spongeEvents.get(ec) != null) {
			return spongeEvents.get(ec).isAssignableFrom(clazz);
		}
		return Utils.classinstanceof(eventClass.get(), clazz);
	}

	/**
	 * Fire the format.
	 * 
	 * In the future, when Events are split into Context and Cause, I will more
	 * effectively support variables in this method.
	 */
	@Listener
	public void onEvent(Event e) {
		if (checkClass(e.getClass())) {
			if (cancel && e instanceof Cancellable) {
				((Cancellable) e).setCancelled(true);
			}
			if (cancel && e instanceof MessageEvent) {
				((MessageEvent) e).setMessageCancelled(true);
			}
			ParsableData data = new ParsableData();
			if (e.getCause().containsNamed("sender")) {
				data.setSender(e.getCause().get("sender", Player.class));
			}
			if (e.getCause().containsNamed("sendfrom")) {
				data.setSender(e.getCause().get("sendfrom", Player.class));
			}
			if (e.getCause().containsNamed("recipient")) {
				data.setRecipient(e.getCause().get("recipient", Player.class));
			}
			if (e.getCause().containsNamed("sendto")) {
				data.setRecipient(e.getCause().get("sendto", Player.class));
			}
			if (e.getCause().containsNamed("observer")) {
				data.setObserver(e.getCause().get("observer", Player.class));
			}
			final boolean b = e.getCause().get("broadcast", Boolean.class).orElse(false);
			data.setClickHover(true);
			data.setPage(e.getCause().get("page", Integer.class).orElse(0));
			Map<String, Object> map = Utils.hm();
			if (e.getCause().containsNamed("vars")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> v2 = (Map<String, Object>) e.getCause().get("vars", Map.class).get();
				map.putAll(v2);
			}
			for (Map.Entry<String, Object> key : e.getCause().getNamedCauses().entrySet()) {
				if (key.getKey().equals("vars")) {
					continue;
				}
				if (!(key.getValue() instanceof Player)) {
					map.put(key.getKey(), key.getValue());
				}
			}
			map.putAll(EventUtils.getValues(e, true));
			data.setKnown(map);
			if (needsDelay(e.getClass())) {
				Task.builder().delayTicks(20).execute(() -> {
					if (data.getRecipient().isPresent()) {
						send(data.getRecipient().get(), data, Utils.wrap(data.getSender().orElse(null)),
								Utils.presentUUID(), b);
					} else {
						for (Player p : Sponge.getServer().getOnlinePlayers()) {
							send(p, data, Utils.wrap(data.getSender().orElse(null)), Utils.presentUUID(), b);
						}
					}
				}).submit(Ray.get().getPlugin());
			} else {
				if (data.getRecipient().isPresent()) {
					send(data.getRecipient().get(), data, Utils.wrap(data.getSender().orElse(null)),
							Utils.presentUUID(), b);
				} else {
					for (Player p : Sponge.getServer().getOnlinePlayers()) {
						send(p, data, Utils.wrap(data.getSender().orElse(null)), Utils.wrap(UUID.randomUUID()), b);
					}
				}
			}
		}
	}

	@Override
	public boolean send(MessageReceiver f, Map<String, Object> args, Optional<Object> o, Optional<UUID> u, boolean b) {
		return internal.send(f, args, o, u, b);
	}

	@Override
	public boolean send(MessageReceiver f, ParsableData data, Optional<Object> o, Optional<UUID> u, boolean b) {
		return internal.send(f, data, o, u, b);
	}

	@Override
	public Prompt getConversationBuilder(Prompt returnTo, ConversationContext context) {
		return new Prompt(TextTemplate.of("What event would you like to fire this format on?")) {

			@Override
			public Text getQuestion(ConversationContext context) {
				return formatTemplate(context);
			}

			@Override
			public Optional<List<Option>> options(ConversationContext context) {
				return Optional.empty();
			}

			@Override
			public Text getFailedText(ConversationContext context, String failedInput) {
				return Text.of("Yikes! This should not happen!");
			}

			@Override
			public Prompt onInput(Optional<Option> selected, String text, ConversationContext context) {
				ConfigurationNode node = context.getData("node", ConfigurationNode.class, null);
				node.getNode("event").setValue(text);
				return Format.buildConversation(returnTo, context, node);
			}

		};
	}

	@Override
	public boolean hasInternal(Class<? extends Format> clazz, Optional<Integer> index) {
		return internal.getClass().equals(clazz) ? true : internal.hasInternal(clazz, index);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Format> Optional<T> getInternal(Class<T> clazz, Optional<Integer> index) {
		if (!hasInternal(clazz, index)) {
			return Optional.empty();
		}
		if (internal.getClass().equals(clazz)) {
			return Optional.of((T) internal);
		}
		return internal.getInternal(clazz, index);
	}

}
