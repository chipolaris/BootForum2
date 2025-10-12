import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Required for ngModel

import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { PopoverModule, Popover } from 'primeng/popover';

import { APP_PICKER_AVAILABLE_ICONS, AppIconDefinition } from '../shared/hero-icons';

export interface IconSelection {
  iconName: string | null;
  iconColor: string;
}

@Component({
  selector: 'app-icon-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIconComponent, PopoverModule],
  templateUrl: './icon-picker.component.html',
  styleUrls: ['./icon-picker.component.css'],
})
export class IconPickerComponent implements OnInit {
  @Input() initialIconName: string | null = null;
  @Input() initialColor: string = '#333333'; // Default dark gray

  @Output() selectionChanged = new EventEmitter<IconSelection>();

  availableIcons: AppIconDefinition[] = APP_PICKER_AVAILABLE_ICONS;

  selectedIconName: string | null = null;
  selectedColor: string = '#333333';

  @ViewChild('op') popover!: Popover;

  ngOnInit(): void {
    this.selectedIconName = this.initialIconName;
    this.selectedColor = this.initialColor;
  }

  togglePicker(event: Event): void {
    this.popover.toggle(event);
  }

  closePicker(): void {
	  this.popover.hide();
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
  }

  emitSelection(): void {
    this.selectionChanged.emit({
      iconName: this.selectedIconName,
      iconColor: this.selectedColor,
    });
  }
}
