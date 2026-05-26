package com.mysite.sbb.user;

import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.question.Question;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
public class SiteUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    @Column(unique = true)
    private String email;

    @OneToMany(mappedBy = "author", cascade = CascadeType.REMOVE)
    private List<Question> questionList;

    @OneToMany(mappedBy = "author", cascade = CascadeType.REMOVE)
    private List<Answer> answerList;
}