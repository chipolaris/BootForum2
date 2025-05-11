import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router'; // For routerLink
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroExclamationTriangle, heroArrowLeftStartOnRectangle } from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule, // For routerLink to go back or to dashboard
    NgIcon
  ],
  providers: [
    provideIcons({ heroExclamationTriangle, heroArrowLeftStartOnRectangle })
  ],
  templateUrl: './unauthorized.component.html',
  styleUrls: ['./unauthorized.component.css']
})
export class UnauthorizedComponent {
  constructor() { }
}
