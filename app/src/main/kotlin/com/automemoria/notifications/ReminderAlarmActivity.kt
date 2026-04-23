package com.automemoria.notifications

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.automemoria.ui.theme.AutomemoriaTheme

class ReminderAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val habitId = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_ID).orEmpty()
        val habitName = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_NAME).orEmpty()

        setContent {
            AutomemoriaTheme {
                ReminderAlarmScreen(
                    habitName = habitName.ifBlank { "Your habit" },
                    onDone = {
                        HabitReminderReceiver.sendAction(
                            context = this,
                            action = HabitReminderScheduler.ACTION_DONE,
                            habitId = habitId,
                            habitName = habitName
                        )
                        finish()
                    },
                    onSnooze = {
                        HabitReminderReceiver.sendAction(
                            context = this,
                            action = HabitReminderScheduler.ACTION_SNOOZE,
                            habitId = habitId,
                            habitName = habitName
                        )
                        finish()
                    },
                    onSkip = {
                        HabitReminderReceiver.sendAction(
                            context = this,
                            action = HabitReminderScheduler.ACTION_SKIP,
                            habitId = habitId,
                            habitName = habitName
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderAlarmScreen(
    habitName: String,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Time to check in",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = habitName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSkip,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip")
                }
                Button(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Snooze 15")
                }
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
