import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Required for ngModel

import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { OverlayPanelModule, OverlayPanel } from 'primeng/overlaypanel';

import { APP_PICKER_AVAILABLE_ICONS, APP_ICONS, AppIconDefinition } from '../shared/hero-icons';

export interface IconSelection {
  iconName: string | null;
  iconColor: string;
}

@Component({
  selector: 'app-icon-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIconComponent,OverlayPanelModule],
  templateUrl: './icon-picker.component.html',
  styleUrls: ['./icon-picker.component.css'], // Can be empty if all styles are Tailwind in HTML
  providers: [
    provideIcons(APP_ICONS)
  ],
})
export class IconPickerComponent implements OnInit {
  @Input() initialIconName: string | null = null;
  @Input() initialColor: string = '#333333'; // Default dark gray

  @Output() selectionChanged = new EventEmitter<IconSelection>();

  availableIcons: AppIconDefinition[] = APP_PICKER_AVAILABLE_ICONS;

  selectedIconName: string | null = null;
  selectedColor: string = '#333333';
  // isPickerOpen: boolean = false;
  @ViewChild('op') overlayPanel!: OverlayPanel; // << For programmatic control if needed

  ngOnInit(): void {
    this.selectedIconName = this.initialIconName;
    this.selectedColor = this.initialColor;
  }

  togglePicker(event: Event): void { // Event is needed for OverlayPanel toggle
    // this.isPickerOpen = !this.isPickerOpen; // Old way
    this.overlayPanel.toggle(event); // New way
  }

  closePicker(): void {
	  // this.isPickerOpen = false; // Old way
	  this.overlayPanel.hide(); // New way
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
