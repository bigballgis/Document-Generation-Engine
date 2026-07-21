import type { Component } from 'vue'
import {
  HomeFilled,
  Postcard,
  Document,
  Collection,
  Connection,
  Histogram,
  User,
  UserFilled,
  FolderOpened,
  Lock,
  List,
} from '@element-plus/icons-vue'

/** Icon map for user-facing nav item ids in `NAV_GROUPS`. */
export const NAV_ICON_MAP: Record<string, Component> = {
  dashboard: HomeFilled,
  users: User,
  groups: UserFilled,
  masters: Postcard,
  templates: Document,
  'content-modules': Collection,
  'asset-library': FolderOpened,
  'api-policies': Connection,
  'api-invocations': List,
  audit: Histogram,
  'legal-holds': Lock,
}

export function getNavIcon(itemId: string): Component | undefined {
  return NAV_ICON_MAP[itemId]
}
