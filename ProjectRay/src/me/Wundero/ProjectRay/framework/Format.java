package me.Wundero.ProjectRay.framework;

import java.util.List;

import me.Wundero.ProjectRay.fanciful.FancyMessage;
import me.Wundero.ProjectRay.variables.Variable;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import com.google.common.collect.Lists;

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

//No world-specific formats, all specified under group
public class Format {

	// TODO default formats

	private List<Section> sections;
	private String name;
	private FormatType type;

	public Format(ConfigurationSection section) throws Exception {
		if (section == null) {
			throw new Exception("Configuration Section cannot be null!");
		}
		this.setName(section.getName());
		this.type = FormatType.fromString(name);
		this.sections = Lists.newArrayList();
		for (String s : section.getKeys(false)) {
			if (!(section.get(s) instanceof ConfigurationSection)) {
				continue;
			}
			ConfigurationSection sect = section.getConfigurationSection(s);
			sections.add(Section.createSection(sect));
		}
	}

	public FancyMessage getMessage(OfflinePlayer player, OfflinePlayer[] others) {
		FancyMessage out = new FancyMessage();
		out.getList().clear();
		for (Section s : sections) {
			out.getList().addAll(s.getMessage(player, others).getList());
		}
		return out;
	}

	public List<Section> getSections() {
		return sections;
	}

	public void setSections(List<Section> sections) {
		this.sections = sections;
	}

	public FormatType getType() {
		return type;
	}

	public void setType(FormatType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}