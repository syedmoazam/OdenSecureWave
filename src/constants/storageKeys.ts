/**
 * Storage Keys Constants
 * Centralized location for all storage keys used in the application
 */

export const STORAGE_KEYS = {
  // Currently unused - keeping for future use if needed
  // Remove these if they remain unused after development is complete
} as const;

/**
 * Query Keys for TanStack Query
 * Used for caching and invalidation
 */
export const QUERY_KEYS = {
  // Device queries
  DEVICE_DATA: ['device', 'data'] as const,
  
  // Company queries
  COMPANIES: ['companies'] as const,
  
  // FAQ queries
  FAQ_DATA: ['faq', 'data'] as const,
} as const;

/**
 * Query configuration constants
 */
export const QUERY_CONFIG = {
  // Stale times (how long data is considered fresh)
  STALE_TIME: {
    SHORT: 1000 * 60 * 2,      // 2 minutes
    MEDIUM: 1000 * 60 * 5,     // 5 minutes
    LONG: 1000 * 60 * 30,      // 30 minutes
    VERY_LONG: 1000 * 60 * 60, // 1 hour
  },
  
  // Cache times (how long data stays in cache)
  CACHE_TIME: {
    SHORT: 1000 * 60 * 5,      // 5 minutes
    MEDIUM: 1000 * 60 * 10,    // 10 minutes
    LONG: 1000 * 60 * 30,      // 30 minutes
    VERY_LONG: 1000 * 60 * 60, // 1 hour
  },
  
  // Retry configuration
  RETRY: {
    DEFAULT: 3,
    CRITICAL: 5,
    NO_RETRY: false,
  },
  
  // Refetch intervals
  REFETCH_INTERVAL: {
    DEVICE_STATUS: 1000 * 60,   // 2 minutes for device status
  },
} as const;
