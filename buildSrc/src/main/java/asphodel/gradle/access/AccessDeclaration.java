package asphodel.gradle.access;

import asphodel.gradle.directives.DirectiveMatcher;

public record AccessDeclaration(Kind kind, TargetType targetType, String fqn, String memberName, String descriptor, String loaderPattern, String versionPattern) {

    public enum Kind {
        ACCESSIBLE("accessible", "public", 1),
        EXTENDABLE("extendable", "public-f", 2),
        MUTABLE("mutable", "public-f", 2);

        private final String word;
        private final String accessTransformerModifier;
        private final int strength;

        Kind(String word, String accessTransformerModifier, int strength) {
            this.word = word;
            this.accessTransformerModifier = accessTransformerModifier;
            this.strength = strength;
        }

        public String word() {
            return word;
        }

        public String accessTransformerModifier() {
            return accessTransformerModifier;
        }

        public int strength() {
            return strength;
        }

        public static Kind fromWord(String word) {
            if (word == null) return null;
            if (word.equalsIgnoreCase("accessible")) return ACCESSIBLE;
            if (word.equalsIgnoreCase("extendable")) return EXTENDABLE;
            if (word.equalsIgnoreCase("mutable")) return MUTABLE;
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
        CLASS("class"),
        METHOD("method"),
        FIELD("field");

        private final String word;

        TargetType(String word) {
            this.word = word;
        }

        public String word() {
            return word;
        }

        public static TargetType fromWord(String word) {
            if (word == null) return null;
            if (word.equalsIgnoreCase("class")) return CLASS;
            if (word.equalsIgnoreCase("method")) return METHOD;
            if (word.equalsIgnoreCase("field")) return FIELD;
            return null;
        }
    }

    public boolean matches(String loader, String version) {
        return DirectiveMatcher.matchesPair(loaderPattern, versionPattern, loader, version);
    }

    public boolean hasLoaderCondition() {
        return loaderPattern != null;
    }

    public boolean hasNoConditions() {
        return loaderPattern == null && versionPattern == null;
    }

    public String dedupKey() {
        return targetType.name() + ':' + fqn + ':' + (memberName == null ? "" : memberName) + ':' + (descriptor == null ? "" : descriptor);
    }

    public int strength() {
        return kind.strength();
    }

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