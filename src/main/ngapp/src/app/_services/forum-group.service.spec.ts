import { TestBed } from '@angular/core/testing';

import { ForumGroupService } from './forum-group.service';

describe('ForumGroupService', () => {
  let service: ForumGroupService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ForumGroupService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
