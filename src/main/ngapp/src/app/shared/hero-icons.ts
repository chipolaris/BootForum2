// src/app/shared-icons.ts
import {
  heroUser,
  heroHome,
  heroCog6Tooth,
  heroBell,
  heroAcademicCap,
  heroArchiveBoxArrowDown,
  heroFaceSmile,
  heroPhoto,
  heroLink,
  heroLockClosed,         // Used by IconPicker, ForumStructureTree, and Login
  heroMapPin,
  heroStar,
  heroChevronDown,        // Used by IconPicker internally
  heroArrowRightEndOnRectangle, // Used by Login
  // Add any other globally used HeroIcons here
  heroGift,
  heroGlobeAlt,
  heroHomeModern,
  heroFilm,
  heroCake,
  heroMusicalNote,
  heroShoppingCart,
  heroScissors,
  heroWifi,
  heroTag,
  heroRocketLaunch,
  heroWrenchScrewdriver,
} from '@ng-icons/heroicons/outline';

import {
  heroUserSolid,
  heroHomeSolid,
  heroCog6ToothSolid,
  heroBellSolid,
  heroAcademicCapSolid,
  heroArchiveBoxArrowDownSolid,
  heroFaceSmileSolid,
  heroPhotoSolid,
  heroLinkSolid,
  heroLockClosedSolid,         // Used by IconPicker, ForumStructureTree, and Login
  heroMapPinSolid,
  heroStarSolid,
  heroChevronDownSolid,        // Used by IconPicker internally
  heroArrowRightEndOnRectangleSolid, // Used by Login
  // Add any other globally used HeroIcons here
  heroGiftSolid,
  heroGlobeAltSolid,
  heroHomeModernSolid,
  heroFilmSolid,
  heroCakeSolid,
  heroMusicalNoteSolid,
  heroShoppingCartSolid,
  heroScissorsSolid,
  heroWifiSolid,
  heroTagSolid,
  heroRocketLaunchSolid,
  heroWrenchScrewdriverSolid,
} from '@ng-icons/heroicons/solid';

/**
 * Defines the structure for an icon that can be picked.
 */
export interface AppIconDefinition {
  displayName: string; // User-friendly name for display in the picker
  name: string;        // The string key used by ng-icon (e.g., "heroUser")
}

/**
 * List of icons available for selection in the IconPickerComponent.
 * The 'name' property MUST match a key in APP_ICON_OBJECT_MAP.
 */
export const APP_PICKER_AVAILABLE_ICONS: AppIconDefinition[] = [
  { displayName: 'User', name: 'heroUserSolid' },
  { displayName: 'Home', name: 'heroHomeSolid' },
  { displayName: 'Settings', name: 'heroCog6ToothSolid' },
  { displayName: 'Bell', name: 'heroBellSolid' },
  { displayName: 'Education', name: 'heroAcademicCapSolid' },
  { displayName: 'Archive', name: 'heroArchiveBoxArrowDownSolid' },
  { displayName: 'Smile', name: 'heroFaceSmileSolid' },
  { displayName: 'Photo', name: 'heroPhotoSolid' },
  { displayName: 'Link', name: 'heroLinkSolid' },
  { displayName: 'Lock', name: 'heroLockClosedSolid' },
  { displayName: 'Map Pin', name: 'heroMapPinSolid' },
  { displayName: 'Star', name: 'heroStarSolid' },

  { displayName: 'Gift', name: 'heroGiftSolid' },
  { displayName: 'Globe Alternative', name: 'heroGlobeAltSolid' },
  { displayName: 'Home Modern', name: 'heroHomeModernSolid' },
  { displayName: 'Film', name: 'heroFilmSolid' },
  { displayName: 'Cake', name: 'heroCakeSolid' },
  { displayName: 'Musical Note', name: 'heroMusicalNoteSolid' },
  { displayName: 'Shopping Cart', name: 'heroShoppingCartSolid' },
  { displayName: 'Scissors', name: 'heroScissorsSolid' },
  { displayName: 'Wifi', name: 'heroWifiSolid' },
  { displayName: 'Tag', name: 'heroTagSolid' },
  { displayName: 'Rocket Launch', name: 'heroRocketLaunchSolid' },
  { displayName: 'Wrench Screwdriver', name: 'heroWrenchScrewdriverSolid' },
];

/**
 * A comprehensive map of all HeroIcon string names to their actual icon objects.
 * This is used by `provideIcons` in any component that needs to render these icons.
 * It should include all icons from APP_PICKER_AVAILABLE_ICONS plus any
 * icons used internally by components or by other components in the app.
 */
export const APP_ICONS = {
  heroUser,
  heroHome,
  heroCog6Tooth,
  heroBell,
  heroAcademicCap,
  heroArchiveBoxArrowDown,
  heroFaceSmile,
  heroPhoto,
  heroLink,
  heroLockClosed,
  heroMapPin,
  heroStar,
  heroChevronDown, // For IconPicker's internal chevron
  heroArrowRightEndOnRectangle, // For Login component
  heroGift,
  heroGlobeAlt,
  heroHomeModern,
  heroFilm,
  heroCake,
  heroMusicalNote,
  heroShoppingCart,
  heroScissors,
  heroWifi,
  heroTag,
  heroRocketLaunch,
  heroWrenchScrewdriver,
  // Solid icons
  heroUserSolid,
  heroHomeSolid,
  heroCog6ToothSolid,
  heroBellSolid,
  heroAcademicCapSolid,
  heroArchiveBoxArrowDownSolid,
  heroFaceSmileSolid,
  heroPhotoSolid,
  heroLinkSolid,
  heroLockClosedSolid,         // Used by IconPicker, ForumStructureTree, and Login
  heroMapPinSolid,
  heroStarSolid,
  heroChevronDownSolid,        // Used by IconPicker internally
  heroArrowRightEndOnRectangleSolid, // Used by Login
  heroGiftSolid,
  heroGlobeAltSolid,
  heroHomeModernSolid,
  heroFilmSolid,
  heroCakeSolid,
  heroMusicalNoteSolid,
  heroShoppingCartSolid,
  heroScissorsSolid,
  heroWifiSolid,
  heroTagSolid,
  heroRocketLaunchSolid,
  heroWrenchScrewdriverSolid,
  // Add any other globally used HeroIcons here
};
