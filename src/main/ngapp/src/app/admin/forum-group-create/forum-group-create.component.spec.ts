import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForumGroupCreateComponent } from './forum-group-create.component';

describe('ForumGroupCreateComponent', () => {
  let component: ForumGroupCreateComponent;
  let fixture: ComponentFixture<ForumGroupCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForumGroupCreateComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForumGroupCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
