import { useState } from "react";

// ─────────────────────────────────────────────
// MOCK DATA
// ─────────────────────────────────────────────

const MOCK_USER = {
  id: "u1",
  name: "Jane Smith",
  email: "jane.smith@email.com",
  memberSince: "2022",
  membershipTier: "Gold",   // "Standard" | "Gold" | "Platinum"
  savedStores: [3, 7],
  vehicles: [
    { id: "v1", label: "My Car", plate: "ABC 123", type: "sedan" },
  ],
};

const MOCK_CENTRES = [
  { id: "wf-sydney",    name: "Westfield Sydney",          suburb: "Sydney CBD",     address: "188 Pitt St, Sydney NSW 2000" },
  { id: "wf-bondi",     name: "Westfield Bondi Junction",  suburb: "Bondi Junction", address: "500 Oxford St, Bondi Junction NSW 2022" },
  { id: "wf-chatswood", name: "Westfield Chatswood",       suburb: "Chatswood",      address: "1 Anderson St, Chatswood NSW 2067" },
];

const MOCK_STORES = [
  { id: 1, centreId: "wf-sydney",    name: "Zara",        category: "Fashion",       level: "L3", openUntil: "19:00", logo: "👗" },
  { id: 2, centreId: "wf-sydney",    name: "Apple Store", category: "Technology",    level: "L2", openUntil: "19:00", logo: "🍎" },
  { id: 3, centreId: "wf-sydney",    name: "Cotton On",   category: "Fashion",       level: "L1", openUntil: "19:00", logo: "👕" },
  { id: 4, centreId: "wf-bondi",     name: "JB Hi-Fi",    category: "Technology",    level: "L2", openUntil: "20:00", logo: "📺" },
  { id: 5, centreId: "wf-bondi",     name: "Kmart",       category: "Department",    level: "L1", openUntil: "21:00", logo: "🛒" },
  { id: 6, centreId: "wf-bondi",     name: "Sephora",     category: "Beauty",        level: "L1", openUntil: "19:00", logo: "💄" },
  { id: 7, centreId: "wf-chatswood", name: "Uniqlo",      category: "Fashion",       level: "L2", openUntil: "19:00", logo: "🧥" },
  { id: 8, centreId: "wf-chatswood", name: "Nando's",     category: "Food & Dining", level: "LG", openUntil: "21:00", logo: "🍗" },
  { id: 9, centreId: "wf-chatswood", name: "Boost Juice", category: "Food & Dining", level: "L1", openUntil: "18:00", logo: "🥤" },
];

const STORE_CATEGORIES = ["All", "Fashion", "Technology", "Beauty", "Food & Dining", "Department"];

const MOCK_OFFERS = [
  { id: "o1", centreId: "wf-sydney",    storeId: 1, title: "30% off all outerwear",     expiry: "2026-05-31", badge: "Limited" },
  { id: "o2", centreId: "wf-sydney",    storeId: 2, title: "Free AirPods with MacBook", expiry: "2026-06-15", badge: "New"     },
  { id: "o3", centreId: "wf-bondi",     storeId: 6, title: "Buy 2 get 1 free skincare", expiry: "2026-05-20", badge: "Hot"     },
  { id: "o4", centreId: "wf-chatswood", storeId: 7, title: "20% off linen shirts",      expiry: "2026-06-01", badge: null      },
];

const MOCK_EVENTS = [
  { id: "e1", centreId: "wf-sydney",    title: "Pop-up Farmers Market",   date: "2026-05-18", time: "09:00–15:00", location: "Level G Atrium",    category: "Food"          },
  { id: "e2", centreId: "wf-bondi",     title: "Kids School Holiday Art", date: "2026-05-20", time: "10:00–13:00", location: "Kids Zone, Level 1", category: "Kids"          },
  { id: "e3", centreId: "wf-chatswood", title: "Live Acoustic Sessions",  date: "2026-05-17", time: "12:00–14:00", location: "Centre Court",       category: "Entertainment" },
];

// ─────────────────────────────────────────────
// PARKING MOCK DATA
// ─────────────────────────────────────────────

const MOCK_PARKING_ZONES = {
  "wf-sydney": [
    { id: "P1", label: "Level P1 – Entry via Pitt St",    totalSpots: 120, available: 34, accessible: 4, evCharging: 2 },
    { id: "P2", label: "Level P2 – Entry via Market St",  totalSpots: 150, available: 12, accessible: 3, evCharging: 3 },
    { id: "P3", label: "Level P3 – Rooftop",              totalSpots: 80,  available: 61, accessible: 2, evCharging: 0 },
  ],
  "wf-bondi": [
    { id: "PA", label: "Zone A – Oxford St Entry",  totalSpots: 200, available: 88, accessible: 6, evCharging: 4 },
    { id: "PB", label: "Zone B – Grafton St Entry", totalSpots: 180, available: 5,  accessible: 4, evCharging: 2 },
  ],
  "wf-chatswood": [
    { id: "P1", label: "Level P1", totalSpots: 100, available: 22, accessible: 3, evCharging: 2 },
    { id: "P2", label: "Level P2", totalSpots: 100, available: 47, accessible: 3, evCharging: 2 },
  ],
};

// Hourly busyness index 0–100 for today (index = hour of day)
const MOCK_HOURLY_BUSYNESS = {
  "wf-sydney":    [5, 5, 5, 5, 5, 5, 10, 30, 55, 70, 80, 90, 95, 85, 75, 80, 85, 90, 75, 55, 35, 20, 10, 5],
  "wf-bondi":     [5, 5, 5, 5, 5, 5,  8, 20, 45, 65, 78, 88, 92, 85, 80, 82, 85, 88, 70, 50, 30, 18,  8, 5],
  "wf-chatswood": [5, 5, 5, 5, 5, 5,  8, 25, 50, 68, 80, 88, 90, 82, 75, 78, 82, 88, 72, 52, 32, 18,  8, 5],
};

const PARKING_RATES = {
  "wf-sydney":    { first1h: 6.00, per30min: 4.00, dailyMax: 55.00, evDiscount: 0.10 },
  "wf-bondi":     { first1h: 5.00, per30min: 3.50, dailyMax: 45.00, evDiscount: 0.10 },
  "wf-chatswood": { first1h: 4.00, per30min: 3.00, dailyMax: 38.00, evDiscount: 0.10 },
};

// ─────────────────────────────────────────────
// UTILITY HELPERS
// ─────────────────────────────────────────────

function getAvailabilityStatus(available, total) {
  const pct = available / total;
  if (available === 0) return { label: "Full",          status: "full"     };
  if (pct < 0.10)      return { label: "Very Limited",  status: "critical" };
  if (pct < 0.30)      return { label: "Limited",       status: "low"      };
  return                      { label: "Available",     status: "good"     };
}

function getBusynessLabel(index) {
  if (index >= 85) return "Very Busy";
  if (index >= 65) return "Busy";
  if (index >= 40) return "Moderate";
  return "Quiet";
}

function getBestArrivalTime(hourlyData) {
  // Quietest slot in the next 6 hours from now
  const now    = new Date().getHours();
  const window = hourlyData.slice(now, now + 6);
  const minIdx = window.indexOf(Math.min(...window));
  const best   = now + minIdx;
  const suffix = best >= 12 ? "pm" : "am";
  const display = best > 12 ? best - 12 : best === 0 ? 12 : best;
  return `${display}:00 ${suffix}`;
}

function estimateCost(centreId, hours) {
  const rate = PARKING_RATES[centreId];
  if (!rate) return null;
  const raw = rate.first1h + Math.max(0, (hours - 1) * 2 * rate.per30min);
  return Math.min(raw, rate.dailyMax);
}

// ─────────────────────────────────────────────
// SHARED COMPONENTS
// ─────────────────────────────────────────────

function BottomNav({ active, onNavigate }) {
  const tabs = [
    { key: "home",    label: "Home"    },
    { key: "stores",  label: "Stores"  },
    { key: "offers",  label: "Offers"  },
    { key: "parking", label: "Parking" },
    { key: "profile", label: "Profile" },
  ];
  return (
    <nav aria-label="Main navigation">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          onClick={() => onNavigate(tab.key)}
          aria-current={active === tab.key ? "page" : undefined}
        >
          {tab.label}
        </button>
      ))}
    </nav>
  );
}

function StoreCard({ store, isSaved, onToggleSave, onNavigate }) {
  return (
    <article>
      <span aria-hidden="true">{store.logo}</span>
      <h3>{store.name}</h3>
      <p>{store.category} · {store.level}</p>
      <p>Open until {store.openUntil}</p>
      <button onClick={() => onToggleSave(store.id)}>
        {isSaved ? "Unsave" : "Save store"}
      </button>
      <button onClick={() => onNavigate("storeDetail", { storeId: store.id })}>
        View store
      </button>
    </article>
  );
}

function OfferCard({ offer, store }) {
  return (
    <article>
      {offer.badge && <span>{offer.badge}</span>}
      <h3>{store?.name}</h3>
      <p>{offer.title}</p>
      <p>Expires {offer.expiry}</p>
      <button>Save offer</button>
    </article>
  );
}

function EventCard({ event }) {
  return (
    <article>
      <span>{event.category}</span>
      <h3>{event.title}</h3>
      <p>{event.date} · {event.time}</p>
      <p>{event.location}</p>
      <button>Add to calendar</button>
    </article>
  );
}

// ─────────────────────────────────────────────
// SCREEN: Login
// ─────────────────────────────────────────────

function LoginScreen({ onLogin }) {
  const [email,    setEmail]    = useState("");
  const [password, setPassword] = useState("");
  const [error,    setError]    = useState("");

  function handleLogin() {
    if (!email || !password) {
      setError("Please enter your email and password.");
      return;
    }
    // TODO: replace with real auth call
    onLogin();
  }

  return (
    <div>
      <h1>Westfield</h1>
      <h2>Sign in to your account</h2>

      {error && <p role="alert">{error}</p>}

      <label htmlFor="email">Email</label>
      <input
        id="email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="you@example.com"
        autoComplete="email"
      />

      <label htmlFor="password">Password</label>
      <input
        id="password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Enter your password"
        autoComplete="current-password"
      />

      <button onClick={handleLogin}>Sign In</button>
      <button onClick={() => {}}>Forgot password?</button>
      <button onClick={() => {}}>Create an account</button>
      <button onClick={onLogin}>Continue as guest</button>
    </div>
  );
}

// ─────────────────────────────────────────────
// SCREEN: Home
// ─────────────────────────────────────────────

function HomeScreen({ selectedCentre, onSelectCentre, savedStores, onToggleSave, onNavigate }) {
  const centreOffers = MOCK_OFFERS.filter((o) => o.centreId === selectedCentre.id);
  const centreEvents = MOCK_EVENTS.filter((e) => e.centreId === selectedCentre.id);
  const zones        = MOCK_PARKING_ZONES[selectedCentre.id] || [];
  const totalAvail   = zones.reduce((s, z) => s + z.available, 0);
  const totalSpots   = zones.reduce((s, z) => s + z.totalSpots, 0);
  const { label: availLabel } = getAvailabilityStatus(totalAvail, totalSpots);

  return (
    <div>
      {/* Centre picker */}
      <header>
        <h1>Welcome back, {MOCK_USER.name.split(" ")[0]}</h1>
        <label htmlFor="centreSelect">Your centre</label>
        <select
          id="centreSelect"
          value={selectedCentre.id}
          onChange={(e) => onSelectCentre(e.target.value)}
        >
          {MOCK_CENTRES.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
        <p>{selectedCentre.address}</p>
      </header>

      {/* Parking snapshot */}
      <section>
        <h2>Parking</h2>
        <p>{totalAvail} spots available · {availLabel}</p>
        <button onClick={() => onNavigate("parking")}>Book a spot →</button>
      </section>

      {/* Featured offers */}
      <section>
        <h2>Today's offers</h2>
        {centreOffers.length > 0 ? (
          centreOffers.map((offer) => {
            const store = MOCK_STORES.find((s) => s.id === offer.storeId);
            return <OfferCard key={offer.id} offer={offer} store={store} />;
          })
        ) : (
          <p>No current offers for this centre.</p>
        )}
        <button onClick={() => onNavigate("offers")}>See all offers</button>
      </section>

      {/* Saved stores */}
      {savedStores.length > 0 && (
        <section>
          <h2>Your saved stores</h2>
          {MOCK_STORES.filter((s) => savedStores.includes(s.id)).map((store) => (
            <StoreCard
              key={store.id}
              store={store}
              isSaved
              onToggleSave={onToggleSave}
              onNavigate={onNavigate}
            />
          ))}
        </section>
      )}

      {/* Events */}
      <section>
        <h2>Upcoming events</h2>
        {centreEvents.length > 0 ? (
          centreEvents.map((event) => <EventCard key={event.id} event={event} />)
        ) : (
          <p>No upcoming events at this centre.</p>
        )}
        <button onClick={() => onNavigate("events")}>See all events</button>
      </section>
    </div>
  );
}

// ─────────────────────────────────────────────
// SCREEN: Store Directory
// ─────────────────────────────────────────────

function StoresScreen({ selectedCentre, savedStores, onToggleSave, onNavigate }) {
  const [search,   setSearch]   = useState("");
  const [category, setCategory] = useState("All");

  const filtered = MOCK_STORES.filter((s) => {
    const matchesCentre   = s.centreId === selectedCentre.id;
    const matchesCategory = category === "All" || s.category === category;
    const matchesSearch   = s.name.toLowerCase().includes(search.toLowerCase());
    return matchesCentre && matchesCategory && matchesSearch;
  });

  return (
    <div>
      <h1>Store Directory</h1>
      <p>{selectedCentre.name}</p>

      <label htmlFor="storeSearch">Search stores</label>
      <input
        id="storeSearch"
        type="search"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="e.g. Zara, Apple…"
      />

      <fieldset>
        <legend>Filter by category</legend>
        {STORE_CATEGORIES.map((cat) => (
          <button key={cat} onClick={() => setCategory(cat)} aria-pressed={category === cat}>
            {cat}
          </button>
        ))}
      </fieldset>

      <p>{filtered.length} store{filtered.length !== 1 ? "s" : ""} found</p>

      {filtered.length > 0 ? (
        filtered.map((store) => (
          <StoreCard
            key={store.id}
            store={store}
            isSaved={savedStores.includes(store.id)}
            onToggleSave={onToggleSave}
            onNavigate={onNavigate}
          />
        ))
      ) : (
        <p>No stores match your search.</p>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────
// SCREEN: Store Detail
// ─────────────────────────────────────────────

function StoreDetailScreen({ params, savedStores, onToggleSave, onBack }) {
  const store = MOCK_STORES.find((s) => s.id === params?.storeId);
  if (!store) return <p>Store not found. <button onClick={onBack}>Go back</button></p>;

  const storeOffers = MOCK_OFFERS.filter((o) => o.storeId === store.id);

  return (
    <div>
      <button onClick={onBack}>← Back</button>
      <h1>{store.logo} {store.name}</h1>
      <p>{store.category}</p>
      <p>Location: {store.level}</p>
      <p>Open until {store.openUntil}</p>
      <button onClick={() => onToggleSave(store.id)}>
        {savedStores.includes(store.id) ? "Remove from saved" : "Save store"}
      </button>
      <button>Get in-centre directions</button>

      <section>
        <h2>Current offers</h2>
        {storeOffers.length > 0 ? (
          storeOffers.map((offer) => <OfferCard key={offer.id} offer={offer} store={store} />)
        ) : (
          <p>No current offers from this store.</p>
        )}
      </section>
    </div>
  );
}

// ─────────────────────────────────────────────
// SCREEN: Offers
// ─────────────────────────────────────────────

function OffersScreen({ selectedCentre }) {
  const [filter, setFilter] = useState("All");
  const categories = ["All", "Fashion", "Technology", "Beauty", "Food & Dining"];

  const centreOffers = MOCK_OFFERS.filter((o) => o.centreId === selectedCentre.id);
  const filtered = centreOffers.filter((o) => {
    if (filter === "All") return true;
    const store = MOCK_STORES.find((s) => s.id === o.storeId);
    return store?.category === filter;
  });

  return (
    <div>
      <h1>Offers & Deals</h1>
      <p>{selectedCentre.name}</p>

      <fieldset>
        <legend>Category</legend>
        {categories.map((c) => (
          <button key={c} onClick={() => setFilter(c)} aria-pressed={filter === c}>{c}</button>
        ))}
      </fieldset>

      {filtered.length > 0 ? (
        filtered.map((offer) => {
          const store = MOCK_STORES.find((s) => s.id === offer.storeId);
          return <OfferCard key={offer.id} offer={offer} store={store} />;
        })
      ) : (
        <p>No offers in this category right now.</p>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────
// SCREEN: Events
// ─────────────────────────────────────────────

function EventsScreen({ selectedCentre }) {
  const centreEvents = MOCK_EVENTS.filter((e) => e.centreId === selectedCentre.id);
  return (
    <div>
      <h1>Events</h1>
      <p>{selectedCentre.name}</p>
      {centreEvents.length > 0 ? (
        centreEvents.map((event) => <EventCard key={event.id} event={event} />)
      ) : (
        <p>No upcoming events at this centre.</p>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────
// PARKING: Predictive Insights Panel
// ─────────────────────────────────────────────

/**
 * Shown both on the main Parking screen and inside the booking flow.
 * Displays:
 *  - Current busyness level
 *  - Hourly busyness bar chart (all 24 h, current hour highlighted)
 *  - Predictive recommendation: best time to arrive
 *  - Cost estimate for chosen duration
 *  - High-busyness alert
 */
function ParkingInsightsPanel({ centreId, durationHours }) {
  const hourlyData  = MOCK_HOURLY_BUSYNESS[centreId] || [];
  const currentHour = new Date().getHours();
  const currentBusy = hourlyData[currentHour] ?? 0;
  const bestTime    = getBestArrivalTime(hourlyData);
  const estCost     = estimateCost(centreId, durationHours);
  const rates       = PARKING_RATES[centreId];

  return (
    <section aria-label="Parking insights">
      <h2>Parking Insights</h2>

      {/* ── Current busyness ── */}
      <div>
        <h3>Right now</h3>
        <p>
          {getBusynessLabel(currentBusy)} — {currentBusy}% of spots occupied
        </p>
      </div>

      {/* ── Hourly busyness chart ── */}
      <div role="img" aria-label="Hourly parking busyness chart for today">
        <h3>Today's busyness by hour</h3>
        <ol>
          {hourlyData.map((val, hour) => {
            // Only render labels every 3 hours to keep it readable
            if (hour % 3 !== 0) return null;
            const isCurrent = hour === currentHour;
            const suffix     = hour >= 12 ? "pm" : "am";
            const display    = hour === 0 ? 12 : hour > 12 ? hour - 12 : hour;
            return (
              <li key={hour} aria-current={isCurrent ? "true" : undefined}>
                {/* 
                  Apply real bar styling in your CSS layer.
                  The inline width gives the data hook – remove in production styling.
                */}
                <span>{display}{suffix}</span>
                <span
                  style={{ display: "inline-block", width: `${val}%`, height: 8 }}
                  aria-label={`${getBusynessLabel(val)} (${val}%)`}
                />
                <span>{getBusynessLabel(val)}</span>
                {isCurrent && <mark>← now</mark>}
              </li>
            );
          })}
        </ol>
      </div>

      {/* ── Predictive recommendation ── */}
      <div>
        <h3>Best time to arrive</h3>
        <p>
          Based on typical patterns, the quietest window in the next 6 hours
          is around <strong>{bestTime}</strong>.
        </p>
      </div>

      {/* ── Cost estimate ── */}
      {estCost !== null && rates && (
        <div>
          <h3>Estimated parking cost</h3>
          <p>
            For {durationHours} hour{durationHours > 1 ? "s" : ""}:
            approximately <strong>${estCost.toFixed(2)}</strong>
          </p>
          <p>
            First hour: ${rates.first1h.toFixed(2)} ·
            then ${rates.per30min.toFixed(2)} per 30 min ·
            daily max ${rates.dailyMax.toFixed(2)}
          </p>
          {rates.evDiscount > 0 && (
            <p>⚡ EV charging spots: {(rates.evDiscount * 100).toFixed(0)}% discount applies.</p>
          )}
        </div>
      )}

      {/* ── High-busyness alert ── */}
      {currentBusy >= 80 && (
        <p role="alert">
          ⚠️ Parking is very busy right now. Consider arriving at {bestTime} or
          using public transport.
        </p>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────
// PARKING: Booking Flow (3-step)
// ─────────────────────────────────────────────

/**
 * Step 1 – Choose zone
 * Step 2 – Arrival time, duration, spot preferences (+ inline insights)
 * Step 3 – Review & confirm
 * Step 4 – Success confirmation
 */
function ParkingBookingFlow({ centreId, vehicle, onBookingComplete, onCancel }) {
  const [step,            setStep]            = useState(1);
  const [selectedZone,    setSelectedZone]    = useState(null);
  const [arrivalTime,     setArrivalTime]     = useState("");
  const [duration,        setDuration]        = useState(2);
  const [needsAccessible, setNeedsAccessible] = useState(false);
  const [needsEV,         setNeedsEV]         = useState(false);
  const [confirmedBooking, setConfirmedBooking] = useState(null);

  const zones   = MOCK_PARKING_ZONES[centreId] || [];
  const estCost = estimateCost(centreId, duration);
  const centre  = MOCK_CENTRES.find((c) => c.id === centreId);

  function handleConfirm() {
    // TODO: POST to real booking API
    const booking = {
      id:              `BK-${Date.now()}`,
      centreId,
      centreName:      centre?.name,
      zone:            selectedZone,
      arrivalTime,
      duration,
      vehicle,
      needsAccessible,
      needsEV,
      estimatedCost:   estCost,
    };
    setConfirmedBooking(booking);
    onBookingComplete(booking);
    setStep(4);
  }

  // ── Step 4: Success ──
  if (step === 4 && confirmedBooking) {
    return (
      <div>
        <h2>Booking Confirmed!</h2>
        <p>Booking ID: {confirmedBooking.id}</p>
        <p>Centre: {confirmedBooking.centreName}</p>
        <p>Zone: {confirmedBooking.zone?.label}</p>
        <p>Arrival: {confirmedBooking.arrivalTime}</p>
        <p>Duration: {confirmedBooking.duration} hr{confirmedBooking.duration > 1 ? "s" : ""}</p>
        <p>Vehicle: {confirmedBooking.vehicle?.label} ({confirmedBooking.vehicle?.plate})</p>
        <p>Estimated cost: ~${confirmedBooking.estimatedCost?.toFixed(2)}</p>
        {confirmedBooking.needsAccessible && <p>♿ Accessible spot reserved</p>}
        {confirmedBooking.needsEV         && <p>⚡ EV charging spot reserved</p>}
        <p>A confirmation has been saved to your bookings.</p>
        <button onClick={onCancel}>Done</button>
      </div>
    );
  }

  return (
    <div>
      <button onClick={onCancel}>✕ Cancel</button>
      <h2>Book a Parking Spot</h2>
      <p>Step {step} of 3 — {centre?.name}</p>

      {/* ── STEP 1: Zone selection ── */}
      {step === 1 && (
        <div>
          <h3>Select a parking zone</h3>
          {zones.map((zone) => {
            const { label: avLabel, status } = getAvailabilityStatus(zone.available, zone.totalSpots);
            const isFull = zone.available === 0;
            return (
              <div key={zone.id}>
                <input
                  type="radio"
                  id={`zone-${zone.id}`}
                  name="zone"
                  value={zone.id}
                  disabled={isFull}
                  checked={selectedZone?.id === zone.id}
                  onChange={() => setSelectedZone(zone)}
                />
                <label htmlFor={`zone-${zone.id}`}>
                  <strong>{zone.label}</strong>
                  <span data-status={status}>
                    {zone.available} spot{zone.available !== 1 ? "s" : ""} · {avLabel}
                  </span>
                  {zone.accessible > 0 && <span> · ♿ {zone.accessible} accessible</span>}
                  {zone.evCharging > 0  && <span> · ⚡ {zone.evCharging} EV charging</span>}
                  {isFull && <span> (Full)</span>}
                </label>
              </div>
            );
          })}
          <button disabled={!selectedZone} onClick={() => setStep(2)}>
            Next: Booking details
          </button>
        </div>
      )}

      {/* ── STEP 2: Booking details + insights ── */}
      {step === 2 && (
        <div>
          <h3>Booking details</h3>
          <p>Zone: {selectedZone?.label}</p>

          <label htmlFor="arrivalTime">Arrival time</label>
          <input
            id="arrivalTime"
            type="time"
            value={arrivalTime}
            onChange={(e) => setArrivalTime(e.target.value)}
          />

          <label htmlFor="duration">Estimated duration</label>
          <select
            id="duration"
            value={duration}
            onChange={(e) => setDuration(Number(e.target.value))}
          >
            {[1, 2, 3, 4, 5, 6, 8, 10].map((h) => (
              <option key={h} value={h}>{h} hr{h > 1 ? "s" : ""}</option>
            ))}
          </select>

          <fieldset>
            <legend>Spot preferences</legend>
            <label>
              <input
                type="checkbox"
                checked={needsAccessible}
                onChange={(e) => setNeedsAccessible(e.target.checked)}
              />
              I need an accessible (♿) spot
            </label>
            <label>
              <input
                type="checkbox"
                checked={needsEV}
                onChange={(e) => setNeedsEV(e.target.checked)}
              />
              I need an EV charging (⚡) spot
            </label>
          </fieldset>

          {/* Predictive insights inline during booking */}
          <ParkingInsightsPanel centreId={centreId} durationHours={duration} />

          <button onClick={() => setStep(1)}>← Back</button>
          <button disabled={!arrivalTime} onClick={() => setStep(3)}>
            Review booking
          </button>
        </div>
      )}

      {/* ── STEP 3: Review & confirm ── */}
      {step === 3 && (
        <div>
          <h3>Review your booking</h3>
          <p>Centre: {centre?.name}</p>
          <p>Zone: {selectedZone?.label}</p>
          <p>Arrival: {arrivalTime}</p>
          <p>Duration: {duration} hr{duration > 1 ? "s" : ""}</p>
          <p>Vehicle: {vehicle?.label} ({vehicle?.plate})</p>
          {needsAccessible && <p>♿ Accessible spot requested</p>}
          {needsEV         && <p>⚡ EV charging spot requested</p>}
          <p>Estimated cost: ~${estCost?.toFixed(2)}</p>
          <p>
            Payment is charged to your saved card on exit.
            Final amount depends on your actual exit time.
          </p>
          <button onClick={() => setStep(2)}>← Back</button>
          <button onClick={handleConfirm}>Confirm booking</button>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────
// SCREEN: Parking (main)
// ─────────────────────────────────────────────

function ParkingScreen({ selectedCentre }) {
  const [view,              setView]              = useState("overview"); // "overview" | "book"
  const [bookings,          setBookings]          = useState([]);
  const [previewDuration,   setPreviewDuration]   = useState(2);

  const zones      = MOCK_PARKING_ZONES[selectedCentre.id] || [];
  const totalAvail = zones.reduce((s, z) => s + z.available, 0);
  const totalSpots = zones.reduce((s, z) => s + z.totalSpots, 0);
  const { label: availLabel, status: availStatus } = getAvailabilityStatus(totalAvail, totalSpots);

  function handleBookingComplete(booking) {
    setBookings((prev) => [booking, ...prev]);
  }

  if (view === "book") {
    return (
      <ParkingBookingFlow
        centreId={selectedCentre.id}
        vehicle={MOCK_USER.vehicles[0]}
        onBookingComplete={handleBookingComplete}
        onCancel={() => setView("overview")}
      />
    );
  }

  return (
    <div>
      <h1>Parking</h1>
      <p>{selectedCentre.name}</p>

      {/* ── Live availability summary ── */}
      <section>
        <h2>Live availability</h2>
        <p data-status={availStatus}>
          {totalAvail} of {totalSpots} spots available · {availLabel}
        </p>

        {zones.map((zone) => {
          const { label: zLabel, status: zStatus } = getAvailabilityStatus(zone.available, zone.totalSpots);
          return (
            <div key={zone.id} data-status={zStatus}>
              <strong>{zone.label}</strong>
              <p>{zone.available} spot{zone.available !== 1 ? "s" : ""} · {zLabel}</p>
              {zone.accessible > 0 && <p>♿ {zone.accessible} accessible spots</p>}
              {zone.evCharging  > 0 && <p>⚡ {zone.evCharging} EV charging spots</p>}
            </div>
          );
        })}

        <button onClick={() => setView("book")}>Book a spot</button>
      </section>

      {/* ── Predictive insights (standalone) ── */}
      <section>
        <label htmlFor="previewDuration">Preview insights for duration:</label>
        <select
          id="previewDuration"
          value={previewDuration}
          onChange={(e) => setPreviewDuration(Number(e.target.value))}
        >
          {[1, 2, 3, 4, 5, 6, 8].map((h) => (
            <option key={h} value={h}>{h} hr{h > 1 ? "s" : ""}</option>
          ))}
        </select>
        <ParkingInsightsPanel centreId={selectedCentre.id} durationHours={previewDuration} />
      </section>

      {/* ── Bookings history ── */}
      {bookings.length > 0 && (
        <section>
          <h2>Your bookings</h2>
          {bookings.map((b) => (
            <div key={b.id}>
              <p>ID: {b.id}</p>
              <p>Zone: {b.zone?.label}</p>
              <p>Arrival: {b.arrivalTime} · {b.duration} hr{b.duration > 1 ? "s" : ""}</p>
              <p>Est. cost: ~${b.estimatedCost?.toFixed(2)}</p>
              {/* TODO: connect cancel to API */}
              <button>Cancel booking</button>
            </div>
          ))}
        </section>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────
// SCREEN: Profile
// ─────────────────────────────────────────────

function ProfileScreen({ onLogout }) {
  return (
    <div>
      <h1>Profile</h1>

      <section>
        <h2>{MOCK_USER.name}</h2>
        <p>{MOCK_USER.email}</p>
        <p>Westfield+ {MOCK_USER.membershipTier} · Member since {MOCK_USER.memberSince}</p>
        <button>Edit profile</button>
      </section>

      <section>
        <h2>My vehicles</h2>
        {MOCK_USER.vehicles.map((v) => (
          <div key={v.id}>
            <p>{v.label} · {v.plate}</p>
            <button>Edit</button>
            <button>Remove</button>
          </div>
        ))}
        <button>Add vehicle</button>
      </section>

      <section>
        <h2>Settings</h2>
        <button>Notification preferences</button>
        <button>Payment methods</button>
        <button>Privacy &amp; data</button>
        <button>Help &amp; support</button>
        <button>About</button>
      </section>

      <button onClick={onLogout}>Sign out</button>
    </div>
  );
}

// ─────────────────────────────────────────────
// ROOT APP
// ─────────────────────────────────────────────

export default function App() {
  const [isLoggedIn,       setIsLoggedIn]       = useState(false);
  const [activeTab,        setActiveTab]        = useState("home");
  const [navStack,         setNavStack]         = useState([]);    // push/pop for sub-screens
  const [selectedCentreId, setSelectedCentreId] = useState("wf-sydney");
  const [savedStores,      setSavedStores]      = useState(MOCK_USER.savedStores);

  const selectedCentre = MOCK_CENTRES.find((c) => c.id === selectedCentreId) || MOCK_CENTRES[0];

  // ── Navigation helpers ──
  function navigate(screen, params = {}) {
    setNavStack((prev) => [...prev, { screen, params }]);
  }
  function goBack() {
    setNavStack((prev) => prev.slice(0, -1));
  }

  // ── Saved store toggle ──
  function toggleSave(storeId) {
    setSavedStores((prev) =>
      prev.includes(storeId) ? prev.filter((id) => id !== storeId) : [...prev, storeId]
    );
  }

  // ── Auth gate ──
  if (!isLoggedIn) {
    return <LoginScreen onLogin={() => setIsLoggedIn(true)} />;
  }

  // ── Sub-screen (pushed onto nav stack) ──
  const currentNav = navStack[navStack.length - 1] || null;
  if (currentNav) {
    const { screen, params } = currentNav;
    if (screen === "storeDetail") {
      return (
        <StoreDetailScreen
          params={params}
          savedStores={savedStores}
          onToggleSave={toggleSave}
          onBack={goBack}
        />
      );
    }
  }

  // ── Tabbed screens ──
  function renderActiveTab() {
    const sharedNav = (tab, params) => {
      if (params) navigate(tab, params);
      else setActiveTab(tab);
    };

    switch (activeTab) {
      case "home":
        return (
          <HomeScreen
            selectedCentre={selectedCentre}
            onSelectCentre={setSelectedCentreId}
            savedStores={savedStores}
            onToggleSave={toggleSave}
            onNavigate={sharedNav}
          />
        );
      case "stores":
        return (
          <StoresScreen
            selectedCentre={selectedCentre}
            savedStores={savedStores}
            onToggleSave={toggleSave}
            onNavigate={navigate}
          />
        );
      case "offers":
        return <OffersScreen selectedCentre={selectedCentre} />;
      case "events":
        return <EventsScreen selectedCentre={selectedCentre} />;
      case "parking":
        return <ParkingScreen selectedCentre={selectedCentre} />;
      case "profile":
        return <ProfileScreen onLogout={() => setIsLoggedIn(false)} />;
      default:
        return null;
    }
  }

  return (
    <div>
      <main>{renderActiveTab()}</main>
      <BottomNav active={activeTab} onNavigate={setActiveTab} />
    </div>
  );
}
