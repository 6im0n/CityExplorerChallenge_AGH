# City Explorer Challenge

An Android app (Kotlin) that turns exploring a city into small challenges.
It reads where you are, finds interesting places nearby (historical sites,
museums, parks), and suggests a personalised set of challenges to visit. When
you reach a place on foot, the app marks the challenge completed.

This is a university project for the AGH MBA Android course.

## What it does

- **Find new challenges:** searches real places around your GPS position and
  shows a list of suggestions, ordered by how well they match you.
- **Current challenges:** the ones you added. Tap one to see its details, pick
  it as your target, and open it on the map.
- **Map with walking route:** shows your live position (blue dot), the place,
  and the walking route between them. The app completes the challenge once you
  get within 30 m.
- **History:** your finished challenges, with stats (how many done, categories
  visited, total distance, most visited city) and an optional photo per place.
- **Challenge details:** explains why each challenge was suggested.

## How places are found

- Places come from the **Geoapify Places** API. The app fetches them live
  around you and stores none in advance. A **Geoapify API key** is required (see
  [Setup](#setup)).
- Walking routes come from the **Geoapify Routing API**.
- The app stores everything you add locally with **Room** (one table; a
  challenge changes state from `current` to `finished`).

## The suggestion system (core of the app)

`ChallengeFinder` generates challenges on the fly. It combines several rules at
once, using your context, rather than reading fixed database entries.

Context used for every search:

- your **current location** and the **distance** to each place,
- the place **category** (historical, museum, park),
- your **history** (how many of each category you already added),
- challenges you **completed recently** (last 24 h),
- the **time of day**.

The rules:

1. **Variety:** the more of a category you already have, the fewer of it the
   app suggests (`weight = 1 / (1 + history)`).
2. **No immediate repetition:** the app cools down a category you finished in
   the last 24 h, so you avoid getting the same thing again.
3. **Time of day:** the app favours outdoor places (parks) in daylight and
   indoor ones (museums) in the evening; historical sites work any time.
4. **Diversity:** every available category gets at least one suggestion.
5. **Distance:** the app prefers closer places and only uses far ones (> 2 km)
   when there are not enough close alternatives.
6. **Novelty:** the app removes places you already added or completed first, so
   nothing repeats.

Each suggestion then gets a **score** equal to `category weight × closeness`.
The app orders the list by score (best match first), and every row shows a
**match %** so the weighting stays visible. The **Challenge details** screen
spells out, in plain words, the data used and which rules made that challenge
appear.

## Tech

- Kotlin, Android Views with ViewBinding, manual `FragmentManager` navigation
- [osmdroid](https://github.com/osmdroid/osmdroid) for the OpenStreetMap map
- Room for local storage, Kotlin coroutines for background work
- `LocationManager` for GPS, `Geocoder` for city names
- Geoapify (Places + Routing)

## Setup

1. Get a free [Geoapify](https://www.geoapify.com/) API key.
2. Add it to `local.properties` (not committed):

   ```
   GEOAPIFY_API_KEY=your_key_here
   ```

3. Open in Android Studio and run on a device or emulator with location
   enabled. A key is required; without one, place search and routes will not
   work.