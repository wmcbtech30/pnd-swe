# Bundled onboarding images

Both originals were supplied in the user's Paninda Google Drive folder and are
stored unchanged in `app/src/main/res/drawable-nodpi/`:

- `onboarding_welcome_background.jpg`: from `onboarding_very_welcome_bgimage.jpg`,
  Drive file `1ZKV_5KEwt57ZrZyEIE8-myj_0IkHkyiL`. Welcome banner only, center-cropped
  with a dark translucent overlay for white-text contrast. Other onboarding
  screens retain their shared blue gradient.
- `google_g_logo.png`: from `GoogleG_Logo.png`, Drive file
  `14BwAITbzw-Wopa_gGOlwx1R1VblYqObr`. User-provided Google Brand Center logo.
  Its original colors/transparency and transparent border are preserved. The
  welcome button centers it in the existing white slot; the visible G is about
  18dp inside the 30dp container.

`ui/onboarding/OnboardingImages.kt` decodes sampled bitmaps off the UI thread so
these high-resolution originals do not inflate to full camera-size memory.
Images are bundled for offline display; the application does not load Drive URLs.
