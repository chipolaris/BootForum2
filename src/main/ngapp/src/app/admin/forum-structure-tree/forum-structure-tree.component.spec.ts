import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForumStructureTreeComponent } from './forum-structure-tree.component';

describe('ForumStructureTreeComponent', () => {
  let component: ForumStructureTreeComponent;
  let fixture: ComponentFixture<ForumStructureTreeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForumStructureTreeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForumStructureTreeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
