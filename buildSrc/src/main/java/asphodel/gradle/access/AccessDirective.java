package asphodel.gradle.access;

import asphodel.gradle.directives.DirectiveMatcher;

public record AccessDirective(Kind kind, TargetType targetType, String fqn, String memberName,
                              String descriptor, String loaderPattern, String versionPattern) {

    public enum Kind {
        ACCESSIBLE("public", 1),
        EXTENDABLE("public-f", 2),
        MUTABLE("public-f", 2);

        private final String accessTransformerModifier;
        private final int strength;

        Kind(String accessTransformerModifier, int strength) {
            this.accessTransformerModifier = accessTransformerModifier;
            this.strength = strength;
        }

        public String word() { return name().toLowerCase(); }
        public String accessTransformerModifier() { return accessTransformerModifier; }
        public int strength() { return strength; }

        public static Kind fromWord(String word) {
            for (Kind kind : values()) if (kind.name().equalsIgnoreCase(word)) return kind;
            return null;
        }

        public boolean validFor(TargetType targetType) {
            return switch (this) {
                case ACCESSIBLE -> true;
                case EXTENDABLE -> targetType != TargetType.FIELD;
                case MUTABLE -> targetType == TargetType.FIELD;
            };
        }
    }

    public enum TargetType {
        CLASS, METHOD, FIELD;

        public String word() { return name().toLowerCase(); }

        public static TargetType fromWord(String word) {
            for (TargetType targetType : values()) if (targetType.name().equalsIgnoreCase(word)) return targetType;
            return null;
        }
    }

    public boolean matches(String loader, String version) {
        return DirectiveMatcher.matches(loaderPattern, loader, false)
                && DirectiveMatcher.matches(versionPattern, version, true);
    }

    public boolean hasLoaderCondition() { return loaderPattern != null; }
    public boolean hasNoConditions() { return loaderPattern == null && versionPattern == null; }

    public String dedupKey() {
        return String.join(":", targetType.name(), fqn, memberName == null ? "" : memberName, descriptor == null ? "" : descriptor);
    }

    public int strength() { return kind.strength(); }

    public String toAccessWidenerLine() {
        String slashFqn = fqn.replace('.', '/');
        return targetType == TargetType.CLASS ? kind.word() + " class " + slashFqn
                : kind.word() + " " + targetType.word() + " " + slashFqn + " " + memberName + " " + descriptor;
    }

    public String toAccessTransformerLine() {
        String modifier = kind.accessTransformerModifier();
        return switch (targetType) {
            case CLASS -> modifier + " " + fqn;
            case METHOD -> modifier + " " + fqn + " " + memberName + descriptor;
            case FIELD -> modifier + " " + fqn + " " + memberName;
        };
    }
}