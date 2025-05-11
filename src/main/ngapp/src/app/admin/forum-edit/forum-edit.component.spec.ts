import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForumEditComponent } from './forum-edit.component';

describe('ForumEditComponent', () => {
  let component: ForumEditComponent;
  let fixture: ComponentFixture<ForumEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForumEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForumEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
