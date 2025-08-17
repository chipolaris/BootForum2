import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputSwitchModule } from 'primeng/inputswitch';
import { IconPickerComponent, IconSelection } from '../../icon-picker/icon-picker.component';
import { TagDTO } from '../../_data/dtos';

@Component({
  selector: 'app-tag-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IconPickerComponent,
    ButtonModule,
    InputTextModule,
    InputSwitchModule
  ],
  templateUrl: './tag-form.component.html',
})
export class TagFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  public ref = inject(DynamicDialogRef);
  public config = inject(DynamicDialogConfig);

  tagForm!: FormGroup;
  isEditMode = false;
  tagToEdit: TagDTO | null = null;

  ngOnInit(): void {
    this.tagToEdit = this.config.data?.tag;
    this.isEditMode = !!this.tagToEdit;

    this.tagForm = this.fb.group({
      label: [this.tagToEdit?.label || '', [Validators.required, Validators.maxLength(100)]],
      icon: [this.tagToEdit?.icon || 'heroTagSolid'],
      iconColor: [this.tagToEdit?.iconColor || '#4f46e5', Validators.required],
      disabled: [this.tagToEdit?.disabled || false]
    });
  }

  get f() { return this.tagForm.controls; }

  handleIconSelection(selection: IconSelection): void {
    this.tagForm.patchValue({
      icon: selection.iconName,
      iconColor: selection.iconColor
    });
  }

  onSubmit(): void {
    if (this.tagForm.invalid) {
      Object.values(this.f).forEach(control => control.markAsTouched());
      return;
    }
    // The form value is the payload for create/update
    this.ref.close(this.tagForm.value);
  }

  onCancel(): void {
    this.ref.close();
  }
}
