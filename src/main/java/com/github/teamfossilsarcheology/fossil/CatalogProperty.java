package com.github.teamfossilsarcheology.fossil;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.*;

/** Preserves decorative state values without retaining obsolete custom block classes. */
public final class CatalogProperty extends Property<String> {
    private final List<String> values;
    public CatalogProperty(String name, List<String> values) { super(name, String.class); this.values = List.copyOf(values); }
    @Override public List<String> getPossibleValues() { return values; }
    @Override public String getName(String value) { return value; }
    @Override public Optional<String> getValue(String value) { return values.contains(value) ? Optional.of(value) : Optional.empty(); }
    @Override public int getInternalIndex(String value) { return values.indexOf(value); }
    @Override public boolean equals(Object other) { return other instanceof CatalogProperty property && super.equals(other) && values.equals(property.values); }
    @Override public int generateHashCode() { return 31 * super.generateHashCode() + values.hashCode(); }
}
