# Consumer ProGuard rules for :feature-key

# Accessibility services are referenced from the manifest only; keep the entry point.
-keep class tech.dotlab.dot.feature.key.service.EssentialKeyAccessibilityService { *; }
