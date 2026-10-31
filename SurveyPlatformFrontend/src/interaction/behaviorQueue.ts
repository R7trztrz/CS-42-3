import type { BehaviorBatch } from './behaviorUploader'

const DB_NAME = 'surveyplatform-behavior'
const DB_VERSION = 1
const STORE_NAME = 'pendingBehaviorBatches'
const SESSION_INDEX = 'sessionId'

export interface StoredBehaviorBatch {
  id: string
  sessionId: string
  createdAt: number
  batch: BehaviorBatch
}

function requestToPromise<T>(request: IDBRequest<T>) {
  return new Promise<T>((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () =>
      reject(request.error ?? new Error('IndexedDB request failed.'))
  })
}

function transactionToPromise(transaction: IDBTransaction) {
  return new Promise<void>((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () =>
      reject(
        transaction.error ??
          new Error('IndexedDB transaction failed.'),
      )
    transaction.onabort = () =>
      reject(
        transaction.error ??
          new Error('IndexedDB transaction was aborted.'),
      )
  })
}

function openDatabase() {
  return new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = () => {
      const db = request.result

      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, {
          keyPath: 'id',
        })

        store.createIndex(SESSION_INDEX, 'sessionId', {
          unique: false,
        })
      }
    }

    request.onsuccess = () => resolve(request.result)
    request.onerror = () =>
      reject(
        request.error ??
          new Error('Unable to open behavior IndexedDB.'),
      )
  })
}

export async function savePendingBehaviorBatch(
  sessionId: string,
  batch: BehaviorBatch,
) {
  const db = await openDatabase()

  try {
    const transaction = db.transaction(
      STORE_NAME,
      'readwrite',
    )

    transaction
      .objectStore(STORE_NAME)
      .put({
        id: batch.batchId,
        sessionId,
        createdAt: Date.now(),
        batch,
      } satisfies StoredBehaviorBatch)

    await transactionToPromise(transaction)
  } finally {
    db.close()
  }
}

export async function deletePendingBehaviorBatch(
  batchId: string,
) {
  const db = await openDatabase()

  try {
    const transaction = db.transaction(
      STORE_NAME,
      'readwrite',
    )

    transaction
      .objectStore(STORE_NAME)
      .delete(batchId)

    await transactionToPromise(transaction)
  } finally {
    db.close()
  }
}

export async function loadPendingBehaviorBatches(
  sessionId: string,
) {
  const db = await openDatabase()

  try {
    const transaction = db.transaction(
      STORE_NAME,
      'readonly',
    )

    const index = transaction
      .objectStore(STORE_NAME)
      .index(SESSION_INDEX)

    const records = await requestToPromise(
      index.getAll(
        IDBKeyRange.only(sessionId),
      ),
    )

    await transactionToPromise(transaction)

    return (
      records as StoredBehaviorBatch[]
    ).sort(
      (a, b) =>
        a.createdAt - b.createdAt,
    )
  } finally {
    db.close()
  }
}
