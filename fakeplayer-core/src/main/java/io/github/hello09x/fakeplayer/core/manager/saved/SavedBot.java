package io.github.hello09x.fakeplayer.core.manager.saved;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public record SavedBot(@NotNull String name, @NotNull Location location) {
}
