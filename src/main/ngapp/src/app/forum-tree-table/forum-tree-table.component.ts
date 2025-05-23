import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TreeNode, MessageService } from 'primeng/api';
import { TreeTableModule } from 'primeng/treetable';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';

// Import NgIconComponent and provideIcons
import { NgIconComponent, provideIcons } from '@ng-icons/core';

// Import the shared icon object map
import { APP_ICONS } from '../shared/hero-icons';

import { ForumGroupService } from '../_services/forum-group.service';
import { ForumGroupDTO, ForumDTO, ApiResponse } from '../_data/dtos';

// Define the structure for the data part of our TreeTable nodes
interface AppTreeTableNodeData {
  id: string; // Unique key for TreeTable, e.g., 'group-1' or 'forum-5'
  title: string;
  type: 'forumGroup' | 'forum';
  discussionCount?: number;
  commentCount?: number;
  lastCommentDate?: Date | null; // Store as Date object for proper formatting
  originalDto: ForumGroupDTO | ForumDTO; // Keep original DTO for reference
}

// Define the TreeTable node structure, extending PrimeNG's TreeNode
interface AppTreeTableNode extends TreeNode {
  data: AppTreeTableNodeData; // TreeNode expects data to be an object with fields
  children?: AppTreeTableNode[];
  leaf?: boolean;
  expanded?: boolean;
}

@Component({
  selector: 'app-forum-group-tree-table',
  standalone: true,
  imports: [
    CommonModule,
    TreeTableModule,
    ProgressSpinnerModule,
    ToastModule,
    NgIconComponent
  ],
  providers: [MessageService, provideIcons(APP_ICONS)], // ForumGroupService is typically providedIn: 'root'
  templateUrl: './forum-tree-table.component.html',
  styleUrls: ['./forum-tree-table.component.css']
})
export class ForumTreeTableComponent implements OnInit {
  treeTableNodes: AppTreeTableNode[] = [];
  isLoading = true;
  errorMessage: string | null = null;

  private forumGroupService = inject(ForumGroupService);
  private messageService = inject(MessageService);

  ngOnInit(): void {
    this.loadForumStructure();
  }

  loadForumStructure(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.forumGroupService.getRootForumGroup().subscribe({
      next: (response: ApiResponse<ForumGroupDTO>) => {
        if (response.success && response.data) {
          const rootGroup = response.data;
          const firstLevelNodes: AppTreeTableNode[] = [];

          // Process first-level children: subGroups
          if (rootGroup.subGroups) {
            firstLevelNodes.push(
              ...rootGroup.subGroups.map(sg => this.transformGroupToNode(sg, true)) // Mark as first-level for expansion
            );
          }
          // Process first-level children: forums
          if (rootGroup.forums) {
            firstLevelNodes.push(
              ...rootGroup.forums.map(f => this.transformForumToNode(f))
            );
          }

          // Sort top-level nodes: groups first, then by title
          firstLevelNodes.sort((a, b) => {
            if (a.data.type === 'forumGroup' && b.data.type === 'forum') return -1;
            if (a.data.type === 'forum' && b.data.type === 'forumGroup') return 1;
            return (a.data.title || '').localeCompare(b.data.title || '');
          });
          this.treeTableNodes = firstLevelNodes;

        } else {
          this.errorMessage = response.message || 'Root forum group not found or failed to load.';
          this.messageService.add({ severity: 'warn', summary: 'Warning', detail: this.errorMessage });
          this.treeTableNodes = []; // Ensure it's empty on failure
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'An unexpected error occurred while fetching forum structure.';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage || 'An unexpected error occurred.' });
        console.error('Error loading forum structure:', err);
        this.treeTableNodes = []; // Ensure it's empty on error
      }
    });
  }

  private transformGroupToNode(group: ForumGroupDTO, isFirstLevel: boolean = false): AppTreeTableNode {
    const nodeData: AppTreeTableNodeData = {
      id: `group-${group.id}`,
      title: group.title,
      type: 'forumGroup',
      originalDto: group
    };

    const children: AppTreeTableNode[] = [];
    if (group.subGroups) {
      children.push(...group.subGroups.map(sg => this.transformGroupToNode(sg, false)));
    }
    if (group.forums) {
      children.push(...group.forums.map(f => this.transformForumToNode(f)));
    }

    children.sort((a, b) => {
        if (a.data.type === 'forumGroup' && b.data.type === 'forum') return -1;
        if (a.data.type === 'forum' && b.data.type === 'forumGroup') return 1;
        return (a.data.title || '').localeCompare(b.data.title || '');
    });

    return {
      data: nodeData,
      children: children.length > 0 ? children : undefined,
      leaf: children.length === 0,
      expanded: isFirstLevel // Expand only the first level groups by default
    };
  }

  private transformForumToNode(forum: ForumDTO): AppTreeTableNode {
    const nodeData: AppTreeTableNodeData = {
      id: `forum-${forum.id}`,
      title: forum.title,
      type: 'forum',
      discussionCount: forum.stat?.discussionCount ?? 0,
      commentCount: forum.stat?.commentCount ?? 0,
      lastCommentDate: forum.stat?.lastComment?.commentDate ? new Date(forum.stat.lastComment.commentDate) : null,
      originalDto: forum
    };

    return {
      data: nodeData,
      leaf: true
    };
  }
}
