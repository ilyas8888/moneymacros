import { usePing } from './usePing'

export function HomePage() {
  const { data, isPending, isError, error } = usePing()

  let apiState: string
  if (isPending) {
    apiState = 'API: checking…'
  } else if (isError) {
    apiState = `API: error (${error.message})`
  } else {
    apiState = `API: ${data.status}`
  }

  return (
    <main>
      <h1>MoneyMacros</h1>
      <p>{apiState}</p>
    </main>
  )
}
