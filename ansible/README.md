# Deployment HOWTO

## Requirement

OS:

- Ubuntu Server 18.04 or CentOS 7/8
- Python installed
- SSH login enabled

## Configuration

### login

Default login user: `ubuntu` (see ansible.cfg)
Has sudo privilege and no password required.
Via boston server.

### hosts

Use internal ip list as hosts.
Use boston server for ssh login.

## Deploy command

Deploy for `production`:

```
$ ./deploy.py --profile production
```
