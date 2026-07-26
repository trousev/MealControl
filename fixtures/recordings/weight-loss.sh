# Recording script: Weight loss profile for an adult male
# Target: 120kg male, 180cm, 35 years old, losing 0.5kg/week
#
# Prerequisites:
#   ./script/introspect emulator start
#   (app should be installed via ./script/introspect install)
#
# Usage:
#   ./script/introspect record -s fixtures/recordings/weight-loss.sh -o fixtures/weight-loss-profile.json
#
# NOTE: Coordinates are for 1080x2400 screen (Medium_Phone_API_36.1).
# If the UI layout changes, re-record using these instructions as a guide.

# Wait for app to be ready after launch
wait-text "Meals"

# Navigate to Settings tab (bottom tab bar, rightmost tab)
# On 1080px wide screen with 3 tabs: x=900, y=2274
tap 900 2274

# Wait for Settings screen to load
wait-text "Working Mode"

# Ensure Automatic mode is selected (tap the "Automatic" radio)
# Typically at ~x=300, y=500
tap 300 500

# --- Body Measurements ---

# Tap Weight field and fill 120
tap 540 650
type "120"

# Tap Height field and fill 180
tap 540 800
type "180"

# Tap Age field and fill 35
tap 540 950
type "35"

# Gender: Male (already default, but tap to confirm)
# Male radio button at ~x=250, y=1100
tap 250 1100

# --- Weight Goal ---

# Tap target weight change field and fill -0.5
tap 540 1300
type "-0.5"

# --- Activity Level ---

# Select "I go for walks sometimes" (Lightly active, multiplier 1.375)
# This is the second option in the activity level list
# Estimate: y=1500
tap 540 1500

# --- Calorie Distribution ---
# Keep default "High Protein (40/30/30)" - no action needed

# --- Hide budget exceeded ---
# Scroll down to reveal more options
swipe 540 1800 540 1200 500

# Tap hide budget exceeded checkbox (estimate: y=1600)
tap 540 1600

# Wait for auto-save (debounce is 300ms)
sleep 2

# Done! The fixture will be dumped automatically by the record command.
