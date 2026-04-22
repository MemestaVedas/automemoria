# Automemoria

*"I will run as fast as I can to wherever my customer desires. I am the Auto Memory Doll, Automemoria."*

---

Automemoria is not a tool of utility, but a vessel for the ephemeral. It is the quiet room where the echoes of your intentions are transcribed into permanence. It exists to capture the fragments of thought, the steady rhythm of your days, and the far-reaching silhouettes of your aspirations.

In a world of fleeting moments, we provide the ink.

---

### The Services of the Doll

#### **I. Transcribing the Heart (Notes)**
Beyond mere records, these are the letters you write to your future self. Linked by the invisible threads of a knowledge graph, every thought finds its place in the grand tapestry of your mind.
*Wikilinks, Backlinks, and the Circular Knowledge Graph.*

#### **II. The Steady Rhythm (Habits)**
Character is formed in the quiet repetition of the everyday. We track the frequency of your soul's pulse, ensuring that no streak is broken and no effort is forgotten.
*Reactive progress and linked synchronization.*

#### **III. The Arrangement of Fate (Kanban)**
To move forward is to organize the chaos. Your ambitions are laid out upon boards of order, moving from the realm of 'concept' to the reality of 'done.'
*Dynamic boards, columns, and prioritized cards.*

#### **IV. The Horizon (Goals)**
Every doll must have a destination. We map the milestones of your journey, calculating the distance remaining between who you are and who you wish to become.
*Linked habits and reactive goal progress.*

---

### The Mechanism

To breathe life into the Doll, one must first align the gears.

#### **1. The Digital Foundation**
The Doll requires a central archive. Configure your Supabase credentials within `local.properties`:
```properties
SUPABASE_URL=https://your-silent-archive.supabase.co
SUPABASE_ANON_KEY=your-secret-cipher-key
```

#### **2. The Soul's Schema**
Initialize the memory banks by executing the `supabase_schema.sql` within your SQL Editor. This prepares the Doll to receive the weight of your words.

#### **3. The Summoning**
Automemoria can be called upon at any moment. Set the app as your **Default Digital Assistant** to evoke the Quick Capture overlay with a simple long-press of the power button.

---

### The Tapestry

```
app/src/main/kotlin/com/automemoria/
├── assist/          # The Summoning API
├── data/
│   ├── local/       # The Deep Archive (Room)
│   ├── remote/      # The Silent Echo (Supabase)
│   └── repository/  # The Bridge of Memory
├── domain/model/    # Pure Intentions
├── ui/
│   ├── home/        # The Dashboard of the Soul
│   ├── graph/       # The Knowledge Web
│   └── theme/       # A Dark-First Aesthetic
└── AutomemoriaApp.kt
```

---

### Technical Specifications

| Component | Library |
|---|---|
| **Language** | Kotlin 2.x |
| **Interface** | Jetpack Compose (Material 3 Expressive) |
| **Logic** | Hilt & StateFlow |
| **Storage** | Room & Supabase |
| **Sync** | WorkManager |

---

*"What is 'love'? I want to know what it means."* 
Transcribe your journey. Find your meaning.
