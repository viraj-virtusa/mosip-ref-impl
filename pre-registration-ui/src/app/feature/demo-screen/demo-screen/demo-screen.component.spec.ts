import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DemoScreenComponent } from './demo-screen.component';

describe('DemoScreenComponent', () => {
  let component: DemoScreenComponent;
  let fixture: ComponentFixture<DemoScreenComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DemoScreenComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DemoScreenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
