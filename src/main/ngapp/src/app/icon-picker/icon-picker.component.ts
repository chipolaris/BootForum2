import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Required for ngModel

import { NgIconComponent, provideIcons } from '@ng-icons/core';
// Import the specific Heroicons you want to offer
import {
  heroUser,
  heroHome,
  heroCog6Tooth, // Updated name for Cog
  heroBell,
  heroAcademicCap,
  heroArchiveBoxArrowDown, // Updated name for ArchiveBox
  heroFaceSmile,
  heroPhoto,
  heroLink,
  heroLockClosed,
  heroMapPin,
  heroStar,
  heroChevronDown,
} from '@ng-icons/heroicons/outline';

// This object maps string names (used by ng-icon) to the actual icon objects
// These are the icons the component will provide for itself
const iconsToProvideInThisComponent = {
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
  heroChevronDown
};

export interface IconSelection {
  iconName: string | null;
  iconColor: string;
}

@Component({
  selector: 'app-icon-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIconComponent],
  templateUrl: './icon-picker.component.html',
  styleUrls: ['./icon-picker.component.css'], // Can be empty if all styles are Tailwind in HTML
  providers: [
    provideIcons(iconsToProvideInThisComponent)
  ],
})
export class IconPickerComponent implements OnInit {
  @Input() initialIconName: string | null = null;
  @Input() initialColor: string = '#333333'; // Default dark gray

  @Output() selectionChanged = new EventEmitter<IconSelection>();

  // List of icons available in the picker
  // The 'name' property MUST match the key in iconsToProvideInThisComponent
  availableIcons: { displayName: string; name: string }[] = [
    { displayName: 'User', name: 'heroUser' },
    { displayName: 'Home', name: 'heroHome' },
    { displayName: 'Settings', name: 'heroCog6Tooth' },
    { displayName: 'Bell', name: 'heroBell' },
    { displayName: 'Education', name: 'heroAcademicCap' },
    { displayName: 'Archive', name: 'heroArchiveBoxArrowDown' },
    { displayName: 'Smile', name: 'heroFaceSmile' },
    { displayName: 'Photo', name: 'heroPhoto' },
    { displayName: 'Link', name: 'heroLink' },
    { displayName: 'Lock', name: 'heroLockClosed' },
    { displayName: 'Map Pin', name: 'heroMapPin' },
    { displayName: 'Star', name: 'heroStar' },
  ];

  selectedIconName: string | null = null;
  selectedColor: string = '#333333';
  isPickerOpen: boolean = false;

  ngOnInit(): void {
    this.selectedIconName = this.initialIconName;
    this.selectedColor = this.initialColor;
  }

  togglePicker(): void {
    this.isPickerOpen = !this.isPickerOpen;
  }

  closePicker(): void {
    this.isPickerOpen = false;
  }

  selectIcon(iconName: string): void {
    this.selectedIconName = iconName;
    this.emitSelection();
  }

  onColorInput(event: Event): void {
    const newColor = (event.target as HTMLInputElement).value;
    this.selectedColor = newColor;
    this.emitSelection();
  }

  clearIcon(): void {
    this.selectedIconName = null;
    this.emitSelection();
    // Optionally close picker or keep it open for color selection
    // this.isPickerOpen = false;
  }

  emitSelection(): void {
    this.selectionChanged.emit({
      iconName: this.selectedIconName,
      iconColor: this.selectedColor,
    });
  }
}
