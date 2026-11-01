type InstallPrompt = Event & { prompt: () => Promise<void> }

let deferredInstall: InstallPrompt | null = null

export function pendingInstall() {
  return deferredInstall
}

export function registerPwa() {
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault()
    deferredInstall = event as InstallPrompt
    window.dispatchEvent(new Event('xtv-installable'))
  })
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js').catch(() => undefined)
    })
  }
}
