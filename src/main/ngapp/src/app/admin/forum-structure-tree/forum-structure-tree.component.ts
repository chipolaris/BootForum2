// src/app/admin/forum-structure-tree/forum-structure-tree.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TreeNode, MessageService } from 'primeng/api';
import { TreeModule } from 'primeng/tree';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button'; // For action buttons
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog'; // For dialogs

import { Router } from '@angular/router';

import { ForumGroupService } from '../../_services/forum-group.service';
import { ForumGroupDTO, ForumDTO, ApiResponse } from '../../_data/dtos';

// Import Create Components
import { ForumCreateComponent } from '../forum-create/forum-create.component';
import { ForumGroupCreateComponent } from '../forum-group-create/forum-group-create.component';

// Import NgIconComponent and provideIcons
import { NgIconComponent, provideIcons } from '@ng-icons/core';
// Import the specific Heroicons you use (match IconPickerComponent)
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
  heroLockClosed,
  heroMapPin,
  heroStar
  // Add any other icons if your DTOs might contain them
} from '@ng-icons/heroicons/outline';

// This object maps string names (used by ng-icon) to the actual icon objects
const iconsToProvideInTree = {
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
  heroStar
};

// Define a more specific TreeNode type for our use case
interface CustomTreeNode extends TreeNode {
  data: ForumGroupDTO | ForumDTO;
  type: 'forumGroup' | 'forum';
  // id is already part of data.id, no need to duplicate here
}

// Define the expected structure for the node select event
interface NodeSelectEvent {
  originalEvent: Event;
  node: TreeNode;
}

@Component({
  selector: 'app-forum-structure-tree',
  standalone: true,
  imports: [
    CommonModule,
    TreeModule,
    ProgressSpinnerModule,
    ToastModule,
    TagModule,
    ButtonModule,
    DynamicDialogModule,
    NgIconComponent
  ],
  providers: [MessageService, DialogService, provideIcons(iconsToProvideInTree)],
  templateUrl: './forum-structure-tree.component.html',
  styleUrls: ['./forum-structure-tree.component.css']
})
export class ForumStructureTreeComponent implements OnInit {
  treeNodes: CustomTreeNode[] = [];
  isLoading = true;
  errorMessage: string | null = null;
  dialogRef: DynamicDialogRef | undefined;

  private forumGroupService = inject(ForumGroupService);
  private messageService = inject(MessageService);
  private router = inject(Router);
  private dialogService = inject(DialogService); // Injected

  ngOnInit(): void {
    this.loadForumStructure();
  }

  loadForumStructure(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.forumGroupService.getRootForumGroup().subscribe({
      next: (response: ApiResponse<ForumGroupDTO>) => {
        if (response.success && response.data) {
          this.treeNodes = [this.transformGroupToTreeNode(response.data, true)];
          if (this.treeNodes.length > 0) {
            this.expandNodeAndChildren(this.treeNodes[0], 2);
          }
        } else {
          this.errorMessage = response.message || 'Root forum group not found or failed to load.';
          this.messageService.add({ severity: 'warn', summary: 'Warning', detail: this.errorMessage });
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'An unexpected error occurred while fetching forum structure.';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage || 'An unexpected error occurred.' });
      }
    });
  }

  private transformGroupToTreeNode(group: ForumGroupDTO, isRoot: boolean = false): CustomTreeNode {
    const node: CustomTreeNode = {
      label: group.title,
      data: group,
      type: 'forumGroup',
      leaf: false,
      expanded: isRoot, // Expand root by default
      children: [],
      expandedIcon: 'pi pi-folder-open',
      collapsedIcon: 'pi pi-folder',
    };

    if (group.forums && group.forums.length > 0) {
      node.children?.push(...group.forums.map(forum => this.transformForumToTreeNode(forum)));
    }

    if (group.subGroups && group.subGroups.length > 0) {
      node.children?.push(...group.subGroups.map(subGroup => this.transformGroupToTreeNode(subGroup)));
    }
    // Sort children: groups first, then forums, then alphabetically
    node.children?.sort((a, b) => {
        if (a.type === 'forumGroup' && b.type === 'forum') return -1;
        if (a.type === 'forum' && b.type === 'forumGroup') return 1;
        return (a.label || '').localeCompare(b.label || '');
    });
    return node;
  }

  private transformForumToTreeNode(forum: ForumDTO): CustomTreeNode {
    return {
      label: forum.title,
      data: forum,
      type: 'forum',
      leaf: true,
      icon: 'pi pi-comments',
    };
  }

  private expandNodeAndChildren(node: TreeNode, maxDepth: number, currentDepth: number = 1): void {
    if (currentDepth > maxDepth) {
      return;
    }
    node.expanded = true;
    if (node.children) {
      for (const child of node.children) {
        this.expandNodeAndChildren(child, maxDepth, currentDepth + 1);
      }
    }
  }

  onNodeSelect(event: NodeSelectEvent): void {
    const selectedNode = event.node as CustomTreeNode;
    // Existing navigation logic...
    // (This can be kept or modified if clicking the node itself should do something different now)
    console.log('Node selected:', selectedNode.data);
    this.messageService.add({
        severity: 'info',
        summary: `${selectedNode.type === 'forumGroup' ? 'Group' : 'Forum'} Selected`,
        detail: selectedNode.label
    });

    // Example: Navigate to edit page on click
    if (selectedNode.type === 'forumGroup') {
      const groupData = selectedNode.data as ForumGroupDTO;
      if (typeof groupData.id === 'number') {
        this.router.navigate(['/app/admin/forum-group-edit', groupData.id]); // Adjusted path
      }
    } else if (selectedNode.type === 'forum') {
      const forumData = selectedNode.data as ForumDTO;
      if (typeof forumData.id === 'number') {
        this.router.navigate(['/app/admin/forum-edit', forumData.id]); // Adjusted path
      }
    }
  }

  // --- Methods for Adding New Nodes ---

  openAddForumDialog(event: MouseEvent, parentGroupNode: CustomTreeNode): void {
    event.stopPropagation(); // Prevent node selection event
    const parentGroupData = parentGroupNode.data as ForumGroupDTO;

    this.dialogRef = this.dialogService.open(ForumCreateComponent, {
      header: `Add New Forum under "${parentGroupData.title}"`,
      width: 'min(90%, 700px)', // Responsive width
      contentStyle: { "max-height": "90vh", "overflow": "auto" },
      baseZIndex: 10000,
      data: { parentGroupId: parentGroupData.id }, // Pass parent ID
      appendTo: 'body'
    });

    this.dialogRef.onClose.subscribe((newForum: ForumDTO | undefined) => {
      if (newForum && newForum.id !== undefined) {
        this.addNodeToTree(parentGroupNode, this.transformForumToTreeNode(newForum));
        this.messageService.add({ severity: 'success', summary: 'Success', detail: `Forum "${newForum.title}" added.` });
      }
    });
  }

  openAddForumGroupDialog(event: MouseEvent, parentGroupNode: CustomTreeNode): void {
    event.stopPropagation(); // Prevent node selection event
    const parentGroupData = parentGroupNode.data as ForumGroupDTO;

    this.dialogRef = this.dialogService.open(ForumGroupCreateComponent, {
      header: `Add New Subgroup under "${parentGroupData.title}"`,
      width: 'min(90%, 700px)', // Responsive width
      contentStyle: { "max-height": "90vh", "overflow": "auto" },
      baseZIndex: 10000,
      data: { parentGroupId: parentGroupData.id } // Pass parent ID
    });

    this.dialogRef.onClose.subscribe((newGroup: ForumGroupDTO | undefined) => {
      if (newGroup && newGroup.id !== undefined) {
        // Ensure the new group node is created correctly (e.g., with empty children array)
        const newGroupNode = this.transformGroupToTreeNode(newGroup);
        this.addNodeToTree(parentGroupNode, newGroupNode);
        this.messageService.add({ severity: 'success', summary: 'Success', detail: `Group "${newGroup.title}" added.` });
      }
    });
  }

  private addNodeToTree(parentNode: CustomTreeNode, newNode: CustomTreeNode): void {
    if (!parentNode.children) {
      parentNode.children = [];
    }
    parentNode.children.push(newNode);
    // Sort children again after adding
    parentNode.children.sort((a, b) => {
        if (a.type === 'forumGroup' && b.type === 'forum') return -1;
        if (a.type === 'forum' && b.type === 'forumGroup') return 1;
        return (a.label || '').localeCompare(b.label || '');
    });

    parentNode.expanded = true; // Ensure parent is expanded
    this.treeNodes = [...this.treeNodes]; // Trigger change detection for the tree
  }

  // Cleanup dialog reference
  ngOnDestroy() {
    if (this.dialogRef) {
      this.dialogRef.close();
    }
  }
}
