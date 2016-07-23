package me.Wundero.ProjectRay;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import me.Wundero.ProjectRay.config.InternalClickAction;
import me.Wundero.ProjectRay.config.InternalHoverAction;
import me.Wundero.ProjectRay.framework.Format;
import me.Wundero.ProjectRay.framework.Groups;
import me.Wundero.ProjectRay.utils.Utils;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

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

public class Ray {

	private static Ray singleton = new Ray();

	private List<Task> asyncTasks = Lists.newArrayList();
	private List<Task> formatTasks = Lists.newArrayList();

	public void registerTask(Task t) {
		if (t.isAsynchronous()) {
			asyncTasks.add(t);
		}
	}

	public boolean hasFormatTask() {
		return !formatTasks.isEmpty();
	}

	public void registerFormatTask(Task t) {
		registerTask(t);
		formatTasks.add(t);
	}

	public void finishFormatTask(Task t) {
		formatTasks.remove(t);
	}

	private Ray() {
	}

	public static Ray get() {
		return singleton;
	}

	private ProjectRay plugin;
	private ConfigurationNode config;
	private Groups groups;
	private Variables vars;
	private boolean loadableSet = false;

	public void load(ProjectRay plugin) {
		this.setPlugin(plugin);
		this.setConfig(plugin.getConfig());
		loadableSet = config.getNode("loadable").getValue() != null;
		this.setVariables(new Variables());
	}

	public void setLoadable(UUID u) {
		if (!loadableSet) {
			try {
				config.getNode("loadable").setValue(TypeToken.of(UUID.class), u);
			} catch (ObjectMappingException e) {
				config.getNode("loadable").setValue(u);
			}
		}
	}

	public void setLoadable(User u) {
		setLoadable(u.getUniqueId());
	}

	public void terminate() {
		for (Task t : asyncTasks) {
			t.cancel();
		}
	}

	public Game getGame() {
		return plugin.getGame();
	}

	public ProjectRay getPlugin() {
		return plugin;
	}

	public Logger getLogger() {
		return getPlugin().getLogger();
	}

	private void setPlugin(ProjectRay plugin) {
		this.plugin = plugin;
	}

	public ConfigurationNode getConfig() {
		return config;
	}

	public void setConfig(ConfigurationNode config) {
		this.config = config;
	}

	public Groups getGroups() {
		return groups;
	}

	public void setGroups(Groups groups) {
		this.groups = groups;
	}

	public Map<String, Object> setVars(Map<String, Object> known, TextTemplate template, Player sender, boolean isRecip,
			Optional<Format> formatUsed, boolean useClickHover) {
		Map<String, Object> out = Maps.newHashMap();
		for (String key : known.keySet()) {
			if (template.getArguments().containsKey(key)) {
				out.put(key, known.get(key));
			}
		}
		ConfigurationNode args = null;
		if (formatUsed.isPresent() && formatUsed.get().getNode().isPresent()) {
			args = formatUsed.get().getNode().get().getNode("format_args", "arguments");
		}
		if (template != null && template.getArguments() != null) {
			for (String key : template.getArguments().keySet()) {
				String k = key;
				if (k.toLowerCase().startsWith("recip_") != isRecip) {
					continue;
				}
				if (!out.containsKey(key)) {
					Object var = getVariables().get(k, sender);
					Object var2 = var;
					if (args != null) {
						Text t = var instanceof Text ? (Text) var : Text.of(var.toString());
						Text.Builder newVar = t.toBuilder();
						if (useClickHover) {
							try {
								InternalClickAction<?> click = args.getNode(key, "click")
										.getValue(TypeToken.of(InternalClickAction.class));
								InternalHoverAction<?> hover = args.getNode(key, "hover")
										.getValue(TypeToken.of(InternalHoverAction.class));
								if (click != null) {
									if (click instanceof InternalClickAction.ATemplate) {
										((InternalClickAction.ATemplate) click).apply(setVars(out,
												(TextTemplate) click.getResult(), sender, isRecip, formatUsed, false));
									}
									click.applyTo(newVar);
								}
								if (hover != null) {
									if (hover instanceof InternalHoverAction.ShowTemplate) {
										((InternalHoverAction.ShowTemplate) hover).apply(setVars(out,
												(TextTemplate) hover.getResult(), sender, isRecip, formatUsed, false));
									}
									hover.applyTo(newVar);
								}
							} catch (Exception e) {
								Utils.printError(e);
							}
						}
						var2 = newVar.build();
					}
					out.put(key, var2);
				}
			}
		}
		return out;
	}

	public Variables getVariables() {
		return vars;
	}

	public void setVariables(Variables vars) {
		this.vars = vars;
	}
}
