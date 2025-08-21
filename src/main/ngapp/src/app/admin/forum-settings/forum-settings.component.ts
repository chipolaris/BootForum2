import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../_services/admin.service';
import { SettingDTO, errorMessageFromApiResponse } from '../../_data/dtos';
import { MessageService } from 'primeng/api';
import { AccordionModule } from 'primeng/accordion';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-forum-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TitleCasePipe,
    AccordionModule,
    InputTextModule,
    InputTextarea,
    InputSwitchModule,
    InputNumberModule,
    DropdownModule,
    ButtonModule
  ],
  templateUrl: './forum-settings.component.html',
  styleUrls: ['./forum-settings.component.css']
})
export class ForumSettingsComponent implements OnInit {
  private adminService = inject(AdminService);
  private messageService = inject(MessageService);

  settingsMap: Map<string, SettingDTO[]> = new Map();
  isLoading = true;
  isSaving = false;
  categories: string[] = [];

  // A map to hold string representations of JSON values for textareas
  jsonStringValues: Map<string, string> = new Map();

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.isLoading = true;
    this.adminService.getForumSettings().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          // The backend sends a plain object, we need to convert it to a Map
          this.settingsMap = new Map(Object.entries(response.data));
          this.categories = Array.from(this.settingsMap.keys());
          this.prepareJsonStringValues();
        } else {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessageFromApiResponse(response) });
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'API Error', detail: 'Failed to fetch forum settings.' });
        this.isLoading = false;
      }
    });
  }

  saveSettings(): void {
    this.isSaving = true;

    if (!this.updateJsonValuesFromString()) {
      this.isSaving = false;
      return; // Stop saving if JSON is invalid
    }

    this.adminService.updateForumSettings(this.settingsMap).subscribe({
      next: (response) => {
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Settings saved successfully.' });
        } else {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessageFromApiResponse(response) });
        }
        this.isSaving = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'API Error', detail: 'Failed to save settings.' });
        this.isSaving = false;
      }
    });
  }

  // START: New method to handle updates from the textarea
  updateJsonStringValue(key: string, value: string): void {
    this.jsonStringValues.set(key, value);
  }
  // END: New method

  private prepareJsonStringValues(): void {
    this.settingsMap.forEach((settings, category) => {
      settings.forEach(setting => {
        if (setting.type === 'json' || setting.type === 'list') {
          const key = `${category}.${setting.key}`;
          this.jsonStringValues.set(key, JSON.stringify(setting.value, null, 2));
        }
      });
    });
  }

  private updateJsonValuesFromString(): boolean {
    for (const [category, settings] of this.settingsMap.entries()) {
      for (const setting of settings) {
        if (setting.type === 'json' || setting.type === 'list') {
          const key = `${category}.${setting.key}`;
          const stringValue = this.jsonStringValues.get(key);
          try {
            setting.value = JSON.parse(stringValue!);
          } catch (e) {
            this.messageService.add({
              severity: 'error',
              summary: 'Invalid JSON',
              detail: `The value for "${setting.label}" is not valid JSON. Please correct it before saving.`
            });
            return false; // Indicate failure
          }
        }
      }
    }
    return true; // Indicate success
  }
}
