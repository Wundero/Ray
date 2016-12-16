package me.Wundero.Ray.commands.tags;
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

import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import me.Wundero.Ray.Ray;
import me.Wundero.Ray.framework.player.RayPlayer;
import me.Wundero.Ray.menu.AllTagsMenu;
import me.Wundero.Ray.menu.TagMenu;
import me.Wundero.Ray.tag.SelectableTag;
import me.Wundero.Ray.utils.Utils;

/**
 * Select a tag.
 */
public class TagSelectCommand implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (!src.hasPermission("ray.tag.select")) {
			throw new CommandException(Text.of(TextColors.RED, "You are not allowed to do that!"));
		}
		if (!(src instanceof Player)) {
			throw new CommandException(Text.of(TextColors.RED, "You must be a player to do that!"));
		}
		if (args.hasAny("tag")) {
			if (args.hasAny("name")) {
				Optional<SelectableTag> ot = Ray.get().getTags().get(((String) args.getOne("tag").get()).toLowerCase(),
						Utils.hm(), SelectableTag.class);
				if (!ot.isPresent()) {
					throw new CommandException(Text.of(TextColors.RED, "That is not a valid tag!"));
				}
				String name = ((String) args.getOne("name").get()).toLowerCase();
				SelectableTag tag = ot.get();
				if (!tag.getObject().containsKey(name)) {
					throw new CommandException(Text.of(TextColors.RED, "That is not a valid tag name!"));
				}
				RayPlayer.get((Player) src).select(tag, name);
				src.sendMessage(Text.of(TextColors.AQUA, "Tag " + name + " selected!"));
			} else {
				Optional<SelectableTag> ot = Ray.get().getTags().get(((String) args.getOne("tag").get()).toLowerCase(),
						Utils.hm(), SelectableTag.class);
				if (!ot.isPresent()) {
					throw new CommandException(Text.of(TextColors.RED, "That is not a valid tag!"));
				}
				RayPlayer.get((Player) src).open(new TagMenu((Player) src, null, ot.get()));
			}
		} else {
			RayPlayer.get((Player) src).open(new AllTagsMenu((Player) src));
		}
		return CommandResult.success();
	}

}