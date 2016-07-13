package me.Wundero.ProjectRay.framework;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import me.Wundero.ProjectRay.utils.Utils;
import ninja.leaping.configurate.ConfigurationNode;

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
public class Groups {

	// String world is lower case
	private final Map<String, List<Group>> groups = Collections.synchronizedMap(new HashMap<String, List<Group>>());

	// passed section: worlds
	// section config: worlds.<worldname>.groups.<groupname>
	public Groups(ConfigurationNode section) {
		for (ConfigurationNode node : section.getChildrenList()) {
			String world = node.getKey().toString();
			ConfigurationNode node2 = node.getNode("groups");
			List<Group> groups = Lists.newArrayList();
			for (ConfigurationNode child : node2.getChildrenList()) {
				groups.add(createGroup(child));
			}
			this.groups.put(world, groups);
		}
		for (String w : groups.keySet()) {
			for (Group g : groups.get(w)) {
				try {
					g.loadParents();
				} catch (Exception e) {
					Utils.printError(e);
				}
			}
		}
	}

	public Group createGroup(ConfigurationNode node) {
		return new Group(node) {
		};
	}

	public List<Group> getGroups(String world) {
		return groups.get(world.toLowerCase());
	}

}
