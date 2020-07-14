#!/bin/sh

set -e

if tmux has-session -t=iceterm 2> /dev/null; then
  tmux attach -t iceterm
  exit
fi

tmux new-session -d -s iceterm -n emacs -x $(tput cols) -y $(tput lines)

tmux split-window -t iceterm:emacs -h

tmux send-keys -t iceterm:emacs.left "emacsclient -nw --eval '(helm-projectile-find-file)'" Enter
tmux send-keys -t iceterm:emacs.right "ranger" Enter

tmux attach -t iceterm:emacs.right
