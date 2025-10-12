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

// Import Create/Edit Components
import { ForumCreateComponent } from '../forum-create/forum-create.component';
import { ForumGroupCreateComponent } from '../forum-group-create/forum-group-create.component';
import { ForumEditComponent } from '../forum-edit/forum-edit.component';
import { ForumGroupEditComponent } from '../forum-group-edit/forum-group-edit.component';

// Import NgIconComponent and provideIcons
import { NgIconComponent, provideIcons } from '@ng-icons/core';

// Define a more specific TreeNode type for our use case
interface CustomTreeNode extends TreeNode {
  data: ForumGroupDTO | ForumDTO;
  type: 'forumGroup' | 'forum';
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
  providers: [MessageService, DialogService],
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
  private dialogService = inject(DialogService);

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
      expanded: isRoot,
      children: [],
      //expandedIcon: 'pi pi-folder-open', // Keep PrimeNG icons for groups if no custom icon
      //collapsedIcon: 'pi pi-folder',
    };

    if (group.forums && group.forums.length > 0) {
      node.children?.push(...group.forums.map(forum => this.transformForumToTreeNode(forum)));
    }

    if (group.subGroups && group.subGroups.length > 0) {
      node.children?.push(...group.subGroups.map(subGroup => this.transformGroupToTreeNode(subGroup)));
    }
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
      //icon: 'pi pi-comments', // Keep PrimeNG icon for forums if no custom icon
    };
  }

  private expandNodeAndChildren(node: TreeNode, maxDepth: number, currentDepth: number = 1): void {
    if (currentDepth > maxDepth) return;
    node.expanded = true;
    if (node.children) {
      for (const child of node.children) {
        this.expandNodeAndChildren(child, maxDepth, currentDepth + 1);
      }
    }
  }

  onNodeSelect(event: NodeSelectEvent): void {
    const selectedNode = event.node as CustomTreeNode;
    this.messageService.add({
        severity: 'info',
        summary: `${selectedNode.type === 'forumGroup' ? 'Group' : 'Forum'} Selected`,
        detail: selectedNode.label
    });

    if (selectedNode.type === 'forumGroup') {
      const groupData = selectedNode.data as ForumGroupDTO;
      if (typeof groupData.id === 'number') {
        // Open dialog
        this.openEditForumGroupDialog(selectedNode, groupData);
      }
    } else if (selectedNode.type === 'forum') {
      const forumData = selectedNode.data as ForumDTO;
      if (typeof forumData.id === 'number') {
        this.openEditForumDialog(selectedNode, forumData);
      }
    }
  }

  // --- Methods for Adding New Nodes ---
  openAddForumDialog(event: MouseEvent, parentGroupNode: CustomTreeNode): void {
    event.stopPropagation();
    const parentGroupData = parentGroupNode.data as ForumGroupDTO;
    this.dialogRef = this.dialogService.open(ForumCreateComponent, {
      header: `Add New Forum under "${parentGroupData.title}"`,
      width: 'min(90%, 700px)',
      contentStyle: { "max-height": "90vh", "overflow": "auto" },
      baseZIndex: 10000,
      data: { parentGroupId: parentGroupData.id },
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
    event.stopPropagation();
    const parentGroupData = parentGroupNode.data as ForumGroupDTO;
    this.dialogRef = this.dialogService.open(ForumGroupCreateComponent, {
      header: `Add New Subgroup under "${parentGroupData.title}"`,
      width: 'min(90%, 700px)',
      contentStyle: { "max-height": "90vh", "overflow": "auto" },
      baseZIndex: 10000,
      data: { parentGroupId: parentGroupData.id },
      appendTo: 'body'
    });
    this.dialogRef.onClose.subscribe((newGroup: ForumGroupDTO | undefined) => {
      if (newGroup && newGroup.id !== undefined) {
        const newGroupNode = this.transformGroupToTreeNode(newGroup);
        this.addNodeToTree(parentGroupNode, newGroupNode);
        this.messageService.add({ severity: 'success', summary: 'Success', detail: `Group "${newGroup.title}" added.` });
      }
    });
  }

  // --- Methods for Editing Nodes ---
  openEditForumDialog(nodeToUpdate: CustomTreeNode, forumData: ForumDTO): void {
    if (typeof forumData.id !== 'number') return;
    this.dialogRef = this.dialogService.open(ForumEditComponent, {
      header: `Edit Forum: "${forumData.title}"`,
      width: 'min(90%, 700px)',
      contentStyle: { "max-height": "90vh", "overflow": "auto" },
      baseZIndex: 10000,
      data: { forumId: forumData.id },
      appendTo: 'body',
      focusOnShow: false,
    });
    this.dialogRef.onClose.subscribe((updatedForum: ForumDTO | undefined) => {
      if (updatedForum && updatedForum.id === nodeToUpdate.data.id) {
        nodeToUpdate.label = updatedForum.title;

        nodeToUpdate.data = { ...nodeToUpdate.data, ...updatedForum };
        this.treeNodes = [...this.treeNodes];
        this.messageService.add({ severity: 'success', summary: 'Success', detail: `Forum "${updatedForum.title}" updated.` });
      }
    });
  }

  // << NEW METHOD for editing Forum Group >>
  openEditForumGroupDialog(nodeToUpdate: CustomTreeNode, groupData: ForumGroupDTO): void {
    if (typeof groupData.id !== 'number') return;

    this.dialogRef = this.dialogService.open(ForumGroupEditComponent, {
      header: `Edit Forum Group: "${groupData.title}"`,
      width: 'min(90%, 700px)',
      contentStyle: { "max-height": "90vh", "overflow": "auto" },
      baseZIndex: 10000,
      data: { forumGroupId: groupData.id }, // Pass forumGroupId to the edit component
      appendTo: 'body',
      focusOnShow: false,
    });

    this.dialogRef.onClose.subscribe((updatedGroup: ForumGroupDTO | undefined) => {
      if (updatedGroup && updatedGroup.id === nodeToUpdate.data.id) {
        // Update the node in the tree
        nodeToUpdate.label = updatedGroup.title;

        nodeToUpdate.data = { ...nodeToUpdate.data, ...updatedGroup }; // Merge updated data

        // Refresh the tree to reflect changes
        this.treeNodes = [...this.treeNodes];

        this.messageService.add({ severity: 'success', summary: 'Success', detail: `Forum Group "${updatedGroup.title}" updated.` });
      }
    });
  }

  private addNodeToTree(parentNode: CustomTreeNode, newNode: CustomTreeNode): void {
    if (!parentNode.children) parentNode.children = [];
    parentNode.children.push(newNode);
    parentNode.children.sort((a, b) => {
        if (a.type === 'forumGroup' && b.type === 'forum') return -1;
        if (a.type === 'forum' && b.type === 'forumGroup') return 1;
        return (a.label || '').localeCompare(b.label || '');
    });
    parentNode.expanded = true;
    this.treeNodes = [...this.treeNodes];
  }

  ngOnDestroy() {
    if (this.dialogRef) {
      this.dialogRef.close();
    }
  }

  private getFlatForumGroup(group: ForumGroupDTO): ForumGroupDTO {
    const { id, title, icon, iconColor } = group;
    return { id, title, icon, iconColor };
  }


}
