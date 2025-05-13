// src/app/admin/forum-structure-tree/forum-structure-tree.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
// Remove TreeNodeSelectEvent from here if it's causing the error
// We will define the event structure directly in the method
import { TreeNode, MessageService } from 'primeng/api'; // Keep TreeNode and MessageService
import { TreeModule } from 'primeng/tree';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';
import { TagModule } from 'primeng/tag'; // For visual cues if needed

import { Router } from '@angular/router';

import { ForumGroupService } from '../../_services/forum-group.service';
import { ForumGroupDTO, ForumDTO, ApiResponse } from '../../_data/dtos';

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
    TagModule
  ],
  providers: [MessageService],
  templateUrl: './forum-structure-tree.component.html',
  styleUrls: ['./forum-structure-tree.component.css']
})
export class ForumStructureTreeComponent implements OnInit {
  treeNodes: CustomTreeNode[] = [];
  isLoading = true;
  errorMessage: string | null = null;

  private forumGroupService = inject(ForumGroupService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  ngOnInit(): void {
    this.loadForumStructure();
  }

  loadForumStructure(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.forumGroupService.getRootForumGroup().subscribe({
      next: (response: ApiResponse<ForumGroupDTO>) => {
        if (response.success && response.data) {
          this.treeNodes = [this.transformGroupToTreeNode(response.data)];
          // Automatically expand the root node and its direct children for better UX
          if (this.treeNodes.length > 0) {
            this.expandNodeAndChildren(this.treeNodes[0], 2); // Expand root and its children (depth 2)
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

  private transformGroupToTreeNode(group: ForumGroupDTO): CustomTreeNode {
    const node: CustomTreeNode = {
      label: group.title,
      data: group,
      type: 'forumGroup',
      leaf: false,
      expanded: false, // Default to collapsed, will be handled by expandNodeAndChildren
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

  // MODIFIED onNodeSelect method
  onNodeSelect(event: NodeSelectEvent): void { // Changed parameter type to the locally defined interface
    const selectedNode = event.node as CustomTreeNode; // Type assertion to CustomTreeNode

    console.log('Node selected:', selectedNode.data);
    this.messageService.add({
        severity: 'info',
        summary: `${selectedNode.type === 'forumGroup' ? 'Group' : 'Forum'} Selected`,
        detail: selectedNode.label
    });

    if (selectedNode.type === 'forumGroup') {
      const groupData = selectedNode.data as ForumGroupDTO;
      if (typeof groupData.id === 'number') {
        // Corrected route based on app.routes.ts for ForumGroupEditComponent
        this.router.navigate(['/app/admin/forum-groups', groupData.id]);
      } else {
        console.error('Selected ForumGroup does not have a valid ID for navigation.');
        this.messageService.add({ severity: 'error', summary: 'Navigation Error', detail: 'Selected group has no ID.' });
      }
    } else if (selectedNode.type === 'forum') {
      const forumData = selectedNode.data as ForumDTO;
      if (typeof forumData.id === 'number') {
        // Corrected route based on app.routes.ts for ForumEditComponent
        this.router.navigate(['/app/admin/forums', forumData.id]);
      } else {
        console.error('Selected Forum does not have a valid ID for navigation.');
        this.messageService.add({ severity: 'error', summary: 'Navigation Error', detail: 'Selected forum has no ID.' });
      }
    }
  }

  // +++ NEW METHOD to process icon name +++
  getDisplayIconName(icon: string | null): string | null {
    if (!icon) {
      return null; // Or an empty string if preferred for template
    }
    // This logic is for converting something like 'heroFolderOpen' to 'folder_open' for Material Icons
    if (icon.startsWith('hero')) {
      return icon.substring(4).toLowerCase().replace(/([A-Z])/g, '_$1').toLowerCase();
    }
    // If not a 'hero' icon, return it as is (assuming it's a direct Material Icon name or other)
    return icon;
  }
}
