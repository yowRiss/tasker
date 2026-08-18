import { onUnmounted, ref } from 'vue'
import type { Note } from '../note.types'

export interface ReminderOption {
  label: string
  offsetMinutes: number
}

export const REMINDER_OPTIONS: ReminderOption[] = [
  { label: 'At time of event', offsetMinutes: 0 },
  { label: '5 minutes before', offsetMinutes: 5 },
  { label: '10 minutes before', offsetMinutes: 10 },
  { label: '15 minutes before', offsetMinutes: 15 },
  { label: '30 minutes before', offsetMinutes: 30 },
  { label: '1 hour before', offsetMinutes: 60 },
  { label: '1 day before', offsetMinutes: 1440 },
]

const triggeredKeys = new Set<string>()

export function useNoteNotifications() {
  const permission = ref<NotificationPermission>(
    typeof Notification !== 'undefined' ? Notification.permission : 'denied',
  )
  let checkInterval: number | undefined

  async function requestPermission() {
    if (typeof Notification === 'undefined') return 'denied'
    if (Notification.permission === 'default') {
      const res = await Notification.requestPermission()
      permission.value = res
      return res
    }
    return Notification.permission
  }

  function formatOffsetLabel(minutes: number): string {
    const option = REMINDER_OPTIONS.find((opt) => opt.offsetMinutes === minutes)
    if (option) return option.label
    if (minutes < 60) return `${minutes}m before`
    if (minutes < 1440) return `${Math.floor(minutes / 60)}h before`
    return `${Math.floor(minutes / 1440)}d before`
  }

  function checkReminders(notes: Note[]) {
    if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return

    const now = Date.now()

    for (const note of notes) {
      if (!note.reminder_at) continue

      const reminderTime = new Date(note.reminder_at).getTime()
      if (isNaN(reminderTime)) continue

      const offsets = note.reminder_offsets?.length ? note.reminder_offsets : [0]

      for (const offset of offsets) {
        const triggerTime = reminderTime - offset * 60 * 1000
        const key = `${note.id}-${reminderTime}-${offset}`

        // Trigger if time is within the last 2 minutes and hasn't been triggered yet
        const diff = now - triggerTime
        if (diff >= 0 && diff <= 120000 && !triggeredKeys.has(key)) {
          triggeredKeys.add(key)
          fireNotification(note, offset)
        }
      }
    }
  }

  function fireNotification(note: Note, offsetMinutes: number) {
    const timingText = offsetMinutes === 0 ? 'Now' : formatOffsetLabel(offsetMinutes)
    const title = `🔔 Note Reminder: ${note.title}`
    const excerpt = note.content_md.replace(/[#*_`>-]/g, ' ').trim().slice(0, 100) || 'Reminder alert'
    const body = `${timingText} • ${excerpt}`

    const notif = new Notification(title, {
      body,
      tag: note.id,
    })

    notif.onclick = () => {
      window.focus()
      window.location.hash = `#/notes/${note.id}`
    }
  }

  function startScheduler(getNotes: () => Note[]) {
    void requestPermission()
    checkReminders(getNotes())
    checkInterval = window.setInterval(() => {
      checkReminders(getNotes())
    }, 15000) // Check every 15 seconds
  }

  onUnmounted(() => {
    if (checkInterval) clearInterval(checkInterval)
  })

  return {
    permission,
    requestPermission,
    formatOffsetLabel,
    checkReminders,
    startScheduler,
  }
}
