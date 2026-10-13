package net.jadenxgamer.elysium_api.impl.util;

import net.minecraft.tags.TagLoader.EntryWithSource;

import java.util.ArrayList;
import java.util.List;

public final class TagParseResult {
    public final List<EntryWithSource> entries = new ArrayList<>();
    public boolean replaced;
}