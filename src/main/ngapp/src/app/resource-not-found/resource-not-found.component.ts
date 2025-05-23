import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router'; // For routerLink
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroQuestionMarkCircle, heroArrowLeftStartOnRectangle } from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-resource-not-found',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule, // For routerLink to go to dashboard
    NgIcon
  ],
  providers: [
    provideIcons({ heroQuestionMarkCircle, heroArrowLeftStartOnRectangle })
  ],
  templateUrl: './resource-not-found.component.html',
  styleUrls: ['./resource-not-found.component.css'] // You can create an empty CSS file or add specific styles
})
export class ResourceNotFoundComponent {
  constructor() { }
}
