import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'

import api from '../../services/api'


type AssetImageContextValue = {
  participationToken?: string
  studyId?: string
}


const AssetImageContext =
  createContext<AssetImageContextValue>({})


const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
    .replace(/\/$/, '')


export function AssetImageProvider({
  children,
  participationToken,
  studyId,
}: AssetImageContextValue & {
  children: React.ReactNode
}) {
  const value = useMemo(
    () => ({
      participationToken,
      studyId,
    }),
    [
      participationToken,
      studyId,
    ],
  )

  return (
    <AssetImageContext.Provider value={value}>
      {children}
    </AssetImageContext.Provider>
  )
}


export function useAssetImage(
  assetId?: string,
) {
  const {
    participationToken,
    studyId,
  } = useContext(AssetImageContext)

  const [source, setSource] =
    useState('')

  useEffect(() => {
    setSource('')

    if (!assetId) {
      return
    }

    if (participationToken) {
      setSource(
        `${API_BASE_URL}/api/participation/${encodeURIComponent(participationToken)}/assets/${encodeURIComponent(assetId)}/content`,
      )

      return
    }

    if (!studyId) {
      return
    }

    let active = true
    let objectUrl = ''

    void api.get<Blob>(
      `/api/studies/${studyId}/assets/${assetId}/content`,
      {
        responseType: 'blob',
      },
    ).then((response) => {
      if (!active) {
        return
      }

      objectUrl = URL.createObjectURL(response.data)
      setSource(objectUrl)
    }).catch(() => {
      if (active) {
        setSource('')
      }
    })

    return () => {
      active = false

      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [
    assetId,
    participationToken,
    studyId,
  ])

  return source
}
