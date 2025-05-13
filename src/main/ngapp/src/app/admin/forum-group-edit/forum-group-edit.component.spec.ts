import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForumGroupEditComponent } from './forum-group-edit.component';

describe('ForumGroupEditComponent', () => {
  let component: ForumGroupEditComponent;
  let fixture: ComponentFixture<ForumGroupEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForumGroupEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForumGroupEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
