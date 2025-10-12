import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router'; // RouterModule for routerLink
import { TreeNode, MessageService } from 'primeng/api';
import { TreeTableModule } from 'primeng/treetable';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';

// Import NgIconComponent and provideIcons
import { NgIconComponent, provideIcons } from '@ng-icons/core';

import { ForumGroupService } from '../_services/forum-group.service';
import { ForumTreeTableDTO, ForumGroupDTO, ForumDTO, ApiResponse } from '../_data/dtos';

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
    RouterModule,
    TreeTableModule,
    ProgressSpinnerModule,
    ToastModule,
    NgIconComponent
  ],
  providers: [MessageService],
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
    this.forumGroupService.getForumTreeTable().subscribe({
      next: (response: ApiResponse<ForumTreeTableDTO>) => {
        if (response.success && response.data) {
          const forumTreeTable = response.data;

          // forumGroups
          if (forumTreeTable.forumGroups) {
            this.treeTableNodes.push(
              ...forumTreeTable.forumGroups.map(sg => this.transformGroupToNode(sg))
            );
          }
          // forums
          if (forumTreeTable.forums) {
            this.treeTableNodes.push(
              ...forumTreeTable.forums.map(f => this.transformForumToNode(f))
            );
          }

          // Sort groups first, then by title
          this.treeTableNodes.sort((a, b) => {
            if (a.data.type === 'forumGroup' && b.data.type === 'forum') return -1;
            if (a.data.type === 'forum' && b.data.type === 'forumGroup') return 1;
            return (a.data.title || '').localeCompare(b.data.title || '');
          });

        } else {
          this.errorMessage = response.message || 'Forum tree table failed to load.';
          this.messageService.add({ severity: 'warn', summary: 'Warning', detail: this.errorMessage });
          this.treeTableNodes = []; // Ensure it's empty on failure
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'An unexpected error occurred while fetching forum tree table.';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage || 'An unexpected error occurred.' });
        console.error('Error loading forum structure:', err);
        this.treeTableNodes = []; // Ensure it's empty on error
      }
    });
  }

  private transformGroupToNode(group: ForumGroupDTO): AppTreeTableNode {
    const nodeData: AppTreeTableNodeData = {
      id: `group-${group.id}`,
      title: group.title,
      type: 'forumGroup',
      originalDto: group
    };

    const children: AppTreeTableNode[] = [];
    if (group.subGroups) {
      children.push(...group.subGroups.map(sg => this.transformGroupToNode(sg)));
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
      expanded: true
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
