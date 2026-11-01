package dev.mekanismponder;

import net.minecraft.resources.ResourceLocation;

public final class GuideResources {
    private GuideResources() {}

    public static ResourceLocation parse(String value) {
        var location = ResourceLocation.tryParse(value);
        if (location == null) throw new IllegalArgumentException("Invalid resource location: " + value);
        return location;
    }

    public static ResourceLocation of(String namespace, String path) {
        return parse(namespace + ":" + path);
    }
}
