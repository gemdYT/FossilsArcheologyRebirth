package com.github.teamfossilsarcheology.fossil;
import java.util.List;
import java.util.Map;

public record BlockProfile(String name, String kind, String sound, Map<String, List<String>> properties, int light) {}
